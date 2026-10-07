package com.myagree.app.payment;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Scan & Pay links and QR codes in the UPI deep-link format every UPI app reads (NPCI's
 * {@code upi://pay?pa=...&pn=...&am=...&cu=INR&tn=...}): opening the link or scanning the code fills in the payee,
 * the amount and a note, so the farmer only confirms with their UPI PIN.
 */
@Component
class UpiQrCodes {

    private static final int QR_SIZE_PX = 360;
    /** Quiet zone around the code, in modules; scanners need some. */
    private static final int QR_MARGIN = 2;
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final String DATA_URL_PREFIX = "data:image/png;base64,";

    /** The UPI deep link that pays {@code amountRupees} to {@code upiId}, e.g. "upi://pay?pa=shop@okaxis&...". */
    String link(String upiId, String payeeName, long amountRupees, String note) {
        // The payee address keeps its "@": UPI apps read "pa" as it is
        return "upi://pay?pa=" + encode(upiId).replace("%40", "@")
                + "&pn=" + encode(payeeName)
                + "&am=" + amountRupees + ".00"
                + "&cu=" + Payment.CURRENCY
                + "&tn=" + encode(note);
    }

    /** The QR code of {@code link} as a PNG data URL, ready for an {@code <img src>}. */
    String qrDataUrl(String link) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, QR_MARGIN,
                    EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name()));
            return DATA_URL_PREFIX + Base64.getEncoder().encodeToString(png(matrix));
        } catch (WriterException e) {
            throw new IllegalStateException("Cannot draw a QR code for a UPI link", e);
        }
    }

    private static byte[] png(BitMatrix matrix) {
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot encode a QR code as PNG", e);
        }
        return out.toByteArray();
    }

    /** UPI apps decode the link as a URL query; spaces are %20, not "+". */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
