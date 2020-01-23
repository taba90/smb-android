package it.geosolutions.savemybike.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


@RunWith(AndroidJUnit4.class)
public class NFCActivityTest {




    final int TECH_NFC_A = 1;
    final String EXTRA_NFC_A_SAK = "sak";    // short (SAK byte value)
    final String EXTRA_NFC_A_ATQA = "atqa";  // byte[2] (ATQA value)

    final int TECH_NFC_B = 2;
    final String EXTRA_NFC_B_APPDATA = "appdata";    // byte[] (Application Data bytes from ATQB/SENSB_RES)
    final String EXTRA_NFC_B_PROTINFO = "protinfo";  // byte[] (Protocol Info bytes from ATQB/SENSB_RES)

    final int TECH_ISO_DEP = 3;
    final String EXTRA_ISO_DEP_HI_LAYER_RESP = "hiresp";  // byte[] (null for NfcA)
    final String EXTRA_ISO_DEP_HIST_BYTES = "histbytes";  // byte[] (null for NfcB)

    final int TECH_NFC_F = 4;
    final String EXTRA_NFC_F_SC = "systemcode";  // byte[] (system code)
    final String EXTRA_NFC_F_PMM = "pmm";        // byte[] (manufacturer bytes)

    final int TECH_NFC_V = 5;
    final String EXTRA_NFC_V_RESP_FLAGS = "respflags";
    final String EXTRA_NFC_V_DSFID = "dsfid";

    final int TECH_NDEF = 6;
    final String EXTRA_NDEF_MSG = "ndefmsg";              // NdefMessage (Parcelable)
    final String EXTRA_NDEF_MAXLENGTH = "ndefmaxlength";  // int (result for getMaxSize())
    final String EXTRA_NDEF_CARDSTATE = "ndefcardstate";  // int (1: read-only, 2: read/write, 3: unknown)
    final String EXTRA_NDEF_TYPE = "ndeftype";            // int (1: T1T, 2: T2T, 3: T3T, 4: T4T, 101: MF Classic, 102: ICODE)

    final int TECH_NDEF_FORMATABLE = 7;

    final int TECH_MIFARE_CLASSIC = 8;

    final int TECH_MIFARE_ULTRALIGHT = 9;
    final String EXTRA_MIFARE_ULTRALIGHT_IS_UL_C = "isulc";

    final int TECH_NFC_BARCODE = 10;
    final String EXTRA_NFC_BARCODE_BARCODE_TYPE = "barcodetype";

    @Rule
    public ActivityTestRule<NFCActivity> activityRule
            = new ActivityTestRule<>(
            NFCActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False to customize the intent


    @Test
    public void testActivity() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, FormatException, InstantiationException {
        // or equivalent to optionally set an explicit receiver
        activityRule.launchActivity(createIntent());
        activityRule.getActivity().getString(0);
        //InstrumentationRegistry.getContext().startActivity(createIntent());
    }

        private Intent createIntent() throws FormatException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
            byte[] tagId = new byte[]{(byte) 0x3F, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0xAB};

        Bundle nfcaBundle = new Bundle();
        nfcaBundle.putByteArray(EXTRA_NFC_A_ATQA, new byte[]{(byte) 0x44, (byte) 0x00}); //ATQA for Type 2 tag
        nfcaBundle.putShort(EXTRA_NFC_A_SAK, (short) 0x00); //SAK for Type 2 tag*/
            Constructor m =Tag.class.getConstructors()[0];
            Bundle ndefBundle = new Bundle();
            ndefBundle.putInt(EXTRA_NDEF_MAXLENGTH, 48);
            ndefBundle.putInt(EXTRA_NDEF_CARDSTATE, 1);
            ndefBundle.putInt(EXTRA_NDEF_TYPE, 2);
            String msg = "this is a ndef message";
            byte[] languageCode;
            byte[] msgBytes;
            try {
                languageCode = "en".getBytes("US-ASCII");
                msgBytes = msg.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }

            byte[] messagePayload = new byte[1 + languageCode.length
                    + msgBytes.length];
            messagePayload[0] = (byte) 0x02;
            System.arraycopy(languageCode, 0, messagePayload, 1,
                    languageCode.length);
            System.arraycopy(msgBytes, 0, messagePayload, 1 + languageCode.length,
                    msgBytes.length);

            NdefMessage message;
            NdefRecord[] records = new NdefRecord[1];
            NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT, new byte[]{}, messagePayload);
            records[0] = textRecord;
            message = new NdefMessage(records);
            ndefBundle.putParcelable(EXTRA_NDEF_MSG, message);
            Tag tag = createMockTag(ndefBundle, tagId);
            Intent techIntent = new Intent(NfcAdapter.ACTION_NDEF_DISCOVERED, null,
                    InstrumentationRegistry.getContext(), NFCActivity.class);
            techIntent.putExtra(NfcAdapter.EXTRA_ID, tagId);
            techIntent.putExtra(NfcAdapter.EXTRA_TAG, tag);
            techIntent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, new NdefMessage[]{message});
            techIntent.setComponent(new ComponentName("it.geosolutions.savemybike.ui.activity", "NFCActivity"));
            techIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            techIntent.setType("text/plain");
            return techIntent;
        }


    private Tag createMockTag(Bundle ndefBundle, byte[] tagId) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor constructor=Tag.class.getConstructors()[0];
        return (Tag) constructor.newInstance(tagId, new int[]{TECH_NDEF},
                new Bundle[]{ ndefBundle}, 0, null);
    }








}
