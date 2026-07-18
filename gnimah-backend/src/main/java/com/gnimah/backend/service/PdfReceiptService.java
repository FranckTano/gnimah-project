package com.gnimah.backend.service;

import com.gnimah.backend.entity.Paiement;
import com.gnimah.backend.entity.Sejour;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfReceiptService {

    @Value("${app.hotel.name}")
    private String hotelName;

    @Value("${app.hotel.address}")
    private String hotelAddress;

    private static final DeviceRgb WINE       = new DeviceRgb(140, 47, 77);
    private static final DeviceRgb WINE_LIGHT = new DeviceRgb(230, 208, 218);
    private static final DeviceRgb DARK       = new DeviceRgb(46, 32, 36);
    private static final DeviceRgb GRAY       = new DeviceRgb(109, 88, 93);
    private static final DeviceRgb MUTED      = new DeviceRgb(169, 154, 157);
    private static final DeviceRgb GREEN      = new DeviceRgb(47, 158, 111);
    private static final DeviceRgb RED        = new DeviceRgb(208, 69, 90);

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateReceipt(Paiement paiement) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A5);
            document.setMargins(36, 44, 36, 44);

            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            Sejour sejour = paiement.getSejour();

            // ---- En-tête hôtel ----
            document.add(new Paragraph(hotelName)
                    .setFont(bold).setFontSize(18).setFontColor(WINE).setMarginBottom(3));
            document.add(new Paragraph(hotelAddress)
                    .setFont(regular).setFontSize(8).setFontColor(MUTED).setMarginBottom(14));

            SolidLine line = new SolidLine(0.5f);
            line.setColor(WINE_LIGHT);
            document.add(new LineSeparator(line).setMarginBottom(14));

            // ---- Titre reçu ----
            document.add(new Paragraph("REÇU DE PAIEMENT")
                    .setFont(bold).setFontSize(14).setFontColor(DARK)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
            document.add(new Paragraph("N° " + sejour.getNumeroRecu())
                    .setFont(regular).setFontSize(10).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

            // ---- Table détails ----
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1.6f}));
            table.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(14);

            addRow(table, "Client",            sejour.getClient().getNomComplet(), regular, bold);
            addRow(table, "Chambre",           sejour.getChambre().getNumero() + " · " + sejour.getChambre().getType().name(), regular, bold);

            if (sejour.getDateEntree() != null) {
                addRow(table, "Date d'entrée", sejour.getDateEntree().format(DATE_FMT), regular, bold);
            }
            if (sejour.getDateSortie() != null) {
                addRow(table, "Date de sortie", sejour.getDateSortie().format(DATE_FMT), regular, bold);
            }
            addRow(table, "Mode de paiement", paiement.getMode().name().replace("_", " "), regular, bold);
            if (paiement.getReferenceTransaction() != null && !paiement.getReferenceTransaction().isBlank()) {
                addRow(table, "Référence transaction", paiement.getReferenceTransaction(), regular, bold);
            }
            addRow(table, "Date du paiement", paiement.getDatePaiement().format(DATETIME_FMT), regular, bold);
            if (paiement.getAgent() != null) {
                addRow(table, "Agent",         paiement.getAgent().getNomComplet(), regular, bold);
            }

            document.add(table);
            document.add(new LineSeparator(line).setMarginBottom(12));

            // ---- Montant encaissé ----
            document.add(new Paragraph("Montant encaissé")
                    .setFont(regular).setFontSize(10).setFontColor(MUTED).setMarginBottom(4));
            document.add(new Paragraph(formatAmount(paiement.getMontant()) + " FCFA")
                    .setFont(bold).setFontSize(26).setFontColor(WINE).setMarginBottom(8));

            // ---- Solde restant ----
            if (sejour.getResteAPayer() != null && sejour.getResteAPayer().compareTo(BigDecimal.ZERO) > 0) {
                document.add(new Paragraph("Reste à payer : " + formatAmount(sejour.getResteAPayer()) + " FCFA")
                        .setFont(regular).setFontSize(11).setFontColor(RED).setMarginBottom(8));
            } else {
                document.add(new Paragraph("✓  Séjour entièrement réglé")
                        .setFont(bold).setFontSize(11).setFontColor(GREEN).setMarginBottom(8));
            }

            // ---- Pied de page ----
            document.add(new LineSeparator(line).setMarginTop(8).setMarginBottom(10));
            document.add(new Paragraph("Merci de votre confiance  ·  " + hotelName)
                    .setFont(regular).setFontSize(8).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();

        } catch (Exception e) {
            log.error("Erreur génération PDF reçu paiement #{}", paiement.getId(), e);
            throw new RuntimeException("Impossible de générer le reçu PDF", e);
        }

        return baos.toByteArray();
    }

    private void addRow(Table table, String label, String value, PdfFont labelFont, PdfFont valueFont) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(labelFont).setFontSize(9).setFontColor(GRAY))
                .setBorder(Border.NO_BORDER).setPadding(5).setPaddingLeft(0));
        table.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "—").setFont(valueFont).setFontSize(9).setFontColor(DARK))
                .setBorder(Border.NO_BORDER).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount.doubleValue()).replace(",", " ");
    }
}
