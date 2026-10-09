package com.gnimah.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Identifiants invalides", LocalDateTime.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Accès refusé", LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        Map<String, Object> response = new HashMap<>();
        response.put("status", 400);
        response.put("message", "Erreur de validation");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(HttpMessageNotReadableException ex) {
        log.warn("Requête mal formée : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Données de la requête invalides (format de date ou champ incorrect)", LocalDateTime.now()));
    }

    /**
     * Valeur hors liste (statut, mode de paiement, type de chambre inconnus…) ou
     * date illisible. C'est une faute de saisie, pas une panne : le client doit
     * recevoir un 400 qu'il peut corriger, et non une « erreur interne ».
     */
    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleValeurInvalide(Exception ex) {
        log.warn("Valeur invalide : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Une valeur transmise n'est pas reconnue. Vérifiez la saisie et réessayez.",
                        LocalDateTime.now()));
    }

    /**
     * Contrainte de la base violée : doublon (même pièce d'identité, même numéro,
     * même identifiant) ou suppression d'une fiche encore référencée par
     * l'historique (séjours, paiements…).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrite(DataIntegrityViolationException ex) {
        log.warn("Contrainte d'intégrité violée : {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Opération impossible : cette information existe déjà ou est encore "
                        + "utilisée ailleurs dans l'historique.", LocalDateTime.now()));
    }

    /** Deux agents ont modifié la même donnée au même instant : il suffit de recommencer. */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleConcurrence(Exception ex) {
        log.warn("Conflit de mise à jour concurrente : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Cette donnée vient d'être modifiée par un autre poste. "
                        + "Rechargez la page et recommencez.", LocalDateTime.now()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFichierTropLourd(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(413, "Fichier trop volumineux.", LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Les exceptions du framework qui portent déjà leur propre statut HTTP
        // (paramètre manquant, méthode non supportée, ressource inconnue…) sont
        // des erreurs du client : sans ce relais, ce gestionnaire générique les
        // transformerait toutes en 500.
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatusCode status = er.getStatusCode();
            if (status.is4xxClientError()) {
                return ResponseEntity.status(status)
                        .body(new ErrorResponse(status.value(), messageClient(status), LocalDateTime.now()));
            }
        }
        // Le détail (message, classe, stack trace) ne doit jamais atteindre le client : il peut révéler
        // des informations internes (requêtes SQL, chemins de fichiers, config...) exploitables par un attaquant.
        // Il est journalisé côté serveur pour le diagnostic ; le client ne reçoit qu'un message générique.
        log.error("Erreur non gérée", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Une erreur interne est survenue. Veuillez réessayer.", LocalDateTime.now()));
    }

    private String messageClient(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Ressource introuvable";
            case 405 -> "Opération non autorisée sur cette ressource";
            case 413 -> "Fichier trop volumineux.";
            case 415 -> "Format de données non pris en charge";
            default -> "Requête invalide : un paramètre est manquant ou incorrect.";
        };
    }
}
