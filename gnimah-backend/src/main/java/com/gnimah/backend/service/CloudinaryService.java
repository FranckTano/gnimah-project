package com.gnimah.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnimah.backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Envoi et suppression de fichiers sur Cloudinary, signés côté serveur.
 *
 * L'alternative — laisser le navigateur téléverser avec un préréglage « non
 * signé » — exposerait le nom du préréglage dans le bundle public : n'importe
 * qui pourrait alors déposer des fichiers sur le compte. Ici le secret ne quitte
 * jamais le serveur, et seul un administrateur authentifié peut déclencher un
 * envoi.
 *
 * Les identifiants sont facultatifs : sans eux le service se déclare
 * indisponible et l'administration reste utilisable pour référencer des URL
 * externes. Cela évite d'imposer un compte Cloudinary pour lancer l'application
 * en local ou en test.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private static final long TAILLE_MAX = 10L * 1024 * 1024;
    private static final List<String> TYPES_IMAGE =
            List.of("image/jpeg", "image/png", "image/webp", "image/avif");

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${app.cloudinary.folder:gnimah/galerie}")
    private String dossier;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Vrai si les trois identifiants sont présents. */
    public boolean estConfigure() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    public record FichierDistant(String url, String urlVignette, String publicId) {}

    /**
     * Téléverse une image et renvoie son URL de livraison.
     *
     * L'URL porte {@code f_auto,q_auto} : Cloudinary sert alors du WebP ou de
     * l'AVIF selon le navigateur, sans que l'application ait à s'en occuper.
     */
    public FichierDistant televerserImage(MultipartFile fichier) {
        if (!estConfigure()) {
            throw new BusinessException(
                    "L'hébergement d'images n'est pas configuré sur ce serveur. "
                            + "Renseignez CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY et CLOUDINARY_API_SECRET, "
                            + "ou saisissez directement l'adresse d'une image déjà en ligne.");
        }
        valider(fichier);

        String publicId = dossier + "/" + System.currentTimeMillis() + "-" + baseNom(fichier);
        long horodatage = System.currentTimeMillis() / 1000;

        Map<String, String> aSigner = new TreeMap<>();
        aSigner.put("overwrite", "true");
        aSigner.put("public_id", publicId);
        aSigner.put("timestamp", String.valueOf(horodatage));

        MultiValueMap<String, Object> corps = new LinkedMultiValueMap<>();
        corps.add("file", ressource(fichier));
        corps.add("api_key", apiKey);
        corps.add("timestamp", String.valueOf(horodatage));
        corps.add("public_id", publicId);
        corps.add("overwrite", "true");
        corps.add("signature", signer(aSigner));

        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.MULTIPART_FORM_DATA);

        JsonNode reponse;
        try {
            String brut = restTemplate.postForObject(
                    "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                    new HttpEntity<>(corps, entetes), String.class);
            reponse = mapper.readTree(brut);
        } catch (RestClientException | IOException e) {
            log.error("Échec de l'envoi vers Cloudinary", e);
            throw new BusinessException("L'envoi de l'image a échoué. Réessayez dans un instant.");
        }

        String idDistant = reponse.path("public_id").asText();
        if (idDistant.isBlank()) {
            throw new BusinessException("Cloudinary n'a pas renvoyé d'identifiant pour ce fichier.");
        }
        return new FichierDistant(
                urlLivraison(idDistant, "f_auto,q_auto"),
                urlLivraison(idDistant, "f_auto,q_auto,w_620,c_limit"),
                idDistant);
    }

    /**
     * Supprime le fichier distant. Un échec est journalisé sans être propagé :
     * la ligne en base doit disparaître même si Cloudinary est momentanément
     * injoignable, sinon l'administrateur se retrouve avec un visuel qu'il ne
     * peut plus retirer de sa galerie.
     */
    public void supprimer(String publicId) {
        if (publicId == null || publicId.isBlank() || !estConfigure()) return;

        long horodatage = System.currentTimeMillis() / 1000;
        Map<String, String> aSigner = new TreeMap<>();
        aSigner.put("public_id", publicId);
        aSigner.put("timestamp", String.valueOf(horodatage));

        MultiValueMap<String, Object> corps = new LinkedMultiValueMap<>();
        corps.add("public_id", publicId);
        corps.add("api_key", apiKey);
        corps.add("timestamp", String.valueOf(horodatage));
        corps.add("signature", signer(aSigner));

        HttpHeaders entetes = new HttpHeaders();
        entetes.setContentType(MediaType.MULTIPART_FORM_DATA);
        try {
            restTemplate.postForObject(
                    "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy",
                    new HttpEntity<>(corps, entetes), String.class);
        } catch (RestClientException e) {
            log.warn("Suppression Cloudinary impossible pour {} — fichier laissé en place", publicId, e);
        }
    }

    private String urlLivraison(String publicId, String transformation) {
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/" + transformation + "/" + publicId;
    }

    private void valider(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier reçu.");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new BusinessException("L'image ne doit pas dépasser 10 Mo.");
        }
        String type = fichier.getContentType();
        if (type == null || !TYPES_IMAGE.contains(type.toLowerCase())) {
            throw new BusinessException("Format non accepté. Utilisez une image JPG, PNG, WebP ou AVIF.");
        }
    }

    /** Nom de fichier assaini : Cloudinary refuse les identifiants exotiques. */
    private String baseNom(MultipartFile fichier) {
        String nom = fichier.getOriginalFilename() == null ? "image" : fichier.getOriginalFilename();
        int point = nom.lastIndexOf('.');
        if (point > 0) nom = nom.substring(0, point);
        nom = java.text.Normalizer.normalize(nom, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (nom.isBlank()) nom = "image";
        return nom.length() > 60 ? nom.substring(0, 60) : nom;
    }

    /** Cloudinary signe en SHA-1 les paramètres triés, secret concaténé à la fin. */
    private String signer(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(k).append('=').append(v);
        });
        sb.append(apiSecret);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 indisponible sur cette JVM", e);
        }
    }

    /** Expose le nom d'origine : sans lui, Cloudinary rejette la partie multipart. */
    private org.springframework.core.io.Resource ressource(MultipartFile fichier) {
        try {
            byte[] octets = fichier.getBytes();
            return new org.springframework.core.io.ByteArrayResource(octets) {
                @Override
                public String getFilename() {
                    return fichier.getOriginalFilename() == null ? "image" : fichier.getOriginalFilename();
                }
            };
        } catch (IOException e) {
            throw new BusinessException("Impossible de lire le fichier envoyé.");
        }
    }
}
