package com.github.kaiwinter.nfcsonos.main.nfc;

import static org.junit.Assert.assertEquals;

import android.nfc.NdefMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link NfcPayloadUtilTest}.
 */
@RunWith(RobolectricTestRunner.class)
public class NfcPayloadUtilTest {

    @Test
    public void createMessageAndParseMessage() {
        // serialize
        NfcPayload payload = new NfcPayload("17");
        NdefMessage ndefMessage = NfcPayloadUtil.createMessage(payload);

        // deserialize
        NfcPayload nfcPayload = NfcPayloadUtil.parseMessage(ndefMessage);

        assertEquals(payload.getFavoriteId(), nfcPayload.getFavoriteId());
    }
}
