package sm.applet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.security.*;
import javacardx.crypto.*;

import javacard.framework.Util;

public class CryptoApplet extends Applet {
	public static final byte MY_CLA = (byte)0x90;
	public static final byte SET_SYM_KEY_CMD = (byte)0x10;
	public static final byte ENCRYPT = (byte)0x20;
	public static final byte DECRYPT = (byte)0x30;
	
	public static byte[] KEY;
	public static final byte KEY_LENGTH = (byte)0x08;
	public static final byte MAX_MESSAGE_LENGTH = (byte)0x10;
	
	public static final byte KEY_NOT_INITIALIZED = (byte)0x55;
	
	public static Cipher CIPHER;
	public static DESKey DESKEY;


	private CryptoApplet(){
		register();
		KEY = new byte[8];
		Util.arrayFillNonAtomic(KEY, (short)0, KEY_LENGTH, (byte)0x00);
		
		CIPHER = Cipher.getInstance(Cipher.ALG_DES_CBC_ISO9797_M1, false);
		DESKEY = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES, false);
	}
	
	public static void install(byte[] byteArray, short offset, byte length) {
		new CryptoApplet();
	}
	
	private void setSymKey(APDU apdu){
		byte buffer[] = apdu.getBuffer();
		byte lc = buffer[ISO7816.OFFSET_LC];
		byte bytesRead = (byte)apdu.setIncomingAndReceive();
		if((lc != KEY_LENGTH) || (bytesRead != KEY_LENGTH))
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		Util.arrayCopy(buffer, (short)ISO7816.OFFSET_CDATA, KEY, (short)0, KEY_LENGTH);
		DESKEY.setKey(KEY, (short)0);
	}
	
	private void encryptDecryptDES(APDU apdu, byte mode){
		if(this.isKeyEmpty())
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		else{
			byte buffer[] = apdu.getBuffer();
			byte lc = buffer[ISO7816.OFFSET_LC];
			byte bytesRead = (byte)apdu.setIncomingAndReceive();
			if((lc > MAX_MESSAGE_LENGTH) || (bytesRead > MAX_MESSAGE_LENGTH))
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			CIPHER.init(DESKEY, mode);
			short len = CIPHER.doFinal(buffer, (short)ISO7816.OFFSET_CDATA, bytesRead, buffer, (short)ISO7816.OFFSET_CDATA);
			apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA,(short) len);
		}
	}
	
	private boolean isKeyEmpty(){
		for(short i = 0 ; i < KEY.length ; i++){
			if(KEY[i] != (byte)0x00)
				return false;
		}
		return true;
	}
	
	public void process(APDU apdu) {
		byte buffer[] = apdu.getBuffer();
		if (buffer[ISO7816.OFFSET_CLA] == MY_CLA) {
			switch (buffer[ISO7816.OFFSET_INS]){
				case SET_SYM_KEY_CMD:
					this.setSymKey(apdu);
					break;
				case ENCRYPT:
					this.encryptDecryptDES(apdu, Cipher.MODE_ENCRYPT);
					break;
				case DECRYPT:
					this.encryptDecryptDES(apdu, Cipher.MODE_DECRYPT);
					break;
				default:
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
		}
	}
}
