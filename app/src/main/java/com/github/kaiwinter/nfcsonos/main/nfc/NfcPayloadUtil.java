package com.github.kaiwinter.nfcsonos.main.nfc;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Converts between a {@link NdefMessage} and {@link NfcPayload}.
 */
public class NfcPayloadUtil {

    /**
     * Creates a {@link NdefMessage} from a {@link NfcPayload}.
     *
     * @param nfcPayload the {@link NfcPayload}
     * @return a {@link NdefMessage}
     */
    public static NdefMessage createMessage(NfcPayload nfcPayload) {
        String jsonPayload = new Gson().toJson(nfcPayload);

        NdefMessage msg = new NdefMessage(
                new NdefRecord[]{
                        NdefRecord.createUri("https://github.com/kaiwinter/nfcsonos/scanned"),
                        NdefRecord.createTextRecord("de", jsonPayload),
                        /*NdefRecord.createApplicationRecord(BuildConfig.APPLICATION_ID)*/}
        );
        return msg;
    }

    /**
     * Parses a {@link NdefMessage} to a {@link NfcPayload}. Unknown types are silently ignored.
     *
     * @param ndefMessage The {@link NdefMessage}
     * @return A {@link NfcPayload} or <code>null</code>
     */
    public static NfcPayload parseMessage(NdefMessage ndefMessage) {
        if (ndefMessage == null) {
            return null;
        }
        NdefRecord[] records = ndefMessage.getRecords();
        for (NdefRecord record : records) {
            if (record.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
                continue;
            }
            if (!Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                continue;
            }
            byte[] payload = record.getPayload();

            boolean isUTF8 = (payload[0] & 0x080) == 0;  //status byte: bit 7 indicates encoding (0 = UTF-8, 1 = UTF-16)
            int languageLength = payload[0] & 0x03F;     //status byte: bits 5..0 indicate length of language code
            int textLength = payload.length - 1 - languageLength;
            String languageCode = new String(payload, 1, languageLength, StandardCharsets.UTF_8);
            String payloadText = new String(payload, 1 + languageLength, textLength, isUTF8 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16);

            try {
                return new Gson().fromJson(payloadText, NfcPayload.class);
            } catch (JsonSyntaxException e) {
                Log.d(NfcPayloadUtil.class.getSimpleName(), "Could not parse JSON", e);
            }

        }
        return null;
    }
}
