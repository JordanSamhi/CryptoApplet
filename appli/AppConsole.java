package sm.appli;

import java.util.Scanner;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public class AppConsole {

	public static final String DELIMITEUR = "==================================================================================";
	public static final Scanner SCANNER = new Scanner(System.in);

	public static final byte[] SELECT_APPLET = {(byte)0x00,(byte)0xa4,(byte)0x04,(byte)0x00,(byte)0x07,(byte)0xee,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xbb,(byte)0x02};
	public static byte[] SEND_KEY = {(byte)0x90,(byte)0x10,(byte)0x00,(byte)0x00,(byte)0x08,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x10};
	public static final byte[] ENCRYPT_PREAMBULE = {(byte)0x90,(byte)0x20,(byte)0x00,(byte)0x00};
	public static final byte[] DECRYPT_PREAMBULE = {(byte)0x90,(byte)0x30,(byte)0x00,(byte)0x00};
	public static final byte MAX_LENGTH_MESSAGE = 0x10;

	public static void main(String[] args) throws CardException {
		
		banner();
		
		System.out.println("[*] Début application");
		
		TerminalFactory defaultTermFactory = TerminalFactory.getDefault();
		CardTerminals terminalList = defaultTermFactory.terminals();
		CardTerminal cardTerminal = (CardTerminal) terminalList.list().get(0);
		cardTerminal.waitForCardPresent(15);

		if(cardTerminal.isCardPresent()){
			System.out.println("[*] Carte présente");
			System.out.println(DELIMITEUR);

			String CONNECTION_PROTOCOL = "*";
			Card card = cardTerminal.connect(CONNECTION_PROTOCOL);

			ATR cardATR = card.getATR();
			byte[] atrBytes = cardATR.getBytes();
			String atrHex = fromBytesToHexString(atrBytes);

			System.out.println("[*] Affichage ATR et canal");
			System.out.println(cardATR.toString());
			System.out.println(atrHex);

			CardChannel cardChannel = card.getBasicChannel();
			System.out.println(cardChannel);
			System.out.println(DELIMITEUR);

			initialiserCle();

			sendApdu(SELECT_APPLET, "Envoi de la selection de l'applet", cardChannel);

			sendApdu(SEND_KEY, "Envoi commande setKey", cardChannel);


			int choix = 0;
			while(true){
				System.out.println("******** MENU ********");
				System.out.println("[*] Choisissez une option : ");
				do{
					System.out.println("[1] Chiffrer un message");
					System.out.println("[2] Déchiffrer un message");
					System.out.println("[3] Quitter");
					try{
						choix = SCANNER.nextInt();
						if(choix != 1 && choix != 2 && choix != 3)
							System.err.println("Entrez 1, 2 ou 3");
					}catch(Exception e){
						System.err.println("Entrez un chiffre");
						SCANNER.next();
					}
				}while(choix != 1 && choix != 2 && choix != 3);
				switch(choix){
				case 1:
					SCANNER.nextLine();
					sendMessageToEncrypt(cardChannel);
					break;
				case 2:
					SCANNER.nextLine();
					sendMessageToDecrypt(cardChannel);
					break;
				case 3:
					System.out.println("Fermeture de l'application");
					System.exit(0);
				}
			}
		}
		else{
			System.out.println("[!] Carte non insérée");
			System.out.println("[*] Fin de l'application");
		}
	}

	public static void initialiserCle(){
		System.out.println("[*] Initialisation clé DES");
		String cle = null;
		byte[] cleBytes = null;
		do{
			System.out.println("Entrez la clé DES (8 caractères exactement): ");
			cle = SCANNER.nextLine();
			cleBytes = cle.getBytes();
			System.out.println("[*] Clé	  : "+cle);
			System.out.println("[*] Bytes : "+fromBytesToHexString(cleBytes));
		}while(cle.length() != 8);

		java.lang.System.arraycopy(cleBytes, 0, SEND_KEY, 5, 8);
		System.out.println(DELIMITEUR);
	}

	public static byte[] sendApdu(byte[] command, String message, CardChannel cardChannel){
		CommandAPDU apdu = new CommandAPDU(command);
		System.out.println("[*] "+message);
		ResponseAPDU reponse = null;
		try {
			reponse = cardChannel.transmit(apdu);
		} catch (CardException e) {
			System.out.println(e);
		}
		System.out.println("--> "+fromBytesToHexString(apdu.getBytes()));
		System.out.println("<-- "+fromBytesToHexString(reponse.getBytes()));
		System.out.println(DELIMITEUR);
		return reponse.getData();
	}

	public static void sendMessageToEncrypt(CardChannel cardChannel){
		System.out.println("[*] Message à chiffrer");
		String message = null;
		byte[] messageBytes = null;
		do{
			System.out.println("Entrez le message à chiffrer (0 < longueur <= 16): ");
			message = SCANNER.nextLine();
		}while(message.length() <= 0 || message.length() > 16);
		messageBytes = message.getBytes();
		System.out.println("[*] Message	  : "+message);
		System.out.println("[*] Bytes : "+fromBytesToHexString(messageBytes));

		int length = ENCRYPT_PREAMBULE.length + 1 + messageBytes.length + 1;
		byte[] encrypt_command = new byte[length];
		encrypt_command[ENCRYPT_PREAMBULE.length] = (byte) messageBytes.length;
		java.lang.System.arraycopy(ENCRYPT_PREAMBULE, 0, encrypt_command, 0, ENCRYPT_PREAMBULE.length);
		java.lang.System.arraycopy(messageBytes, 0, encrypt_command, ENCRYPT_PREAMBULE.length+1, messageBytes.length);
		encrypt_command[length-1] = MAX_LENGTH_MESSAGE;
		System.out.println(DELIMITEUR);
		String reponse = fromBytesToHexString(sendApdu(encrypt_command, "Envoi de la commande pour chiffrer", cardChannel));
		System.out.println("[*] Message chiffré           : "+reponse);
		System.out.println("[*] Message chiffré en ascii  : "+hexToString(reponse));
		System.out.println(DELIMITEUR);
	}

	public static void sendMessageToDecrypt(CardChannel cardChannel){
		System.out.println("[*] Message à déchiffrer");
		String message = null;
		byte[] messageBytes = null;
		do{
			System.out.println("Entrez le message à déchiffrer (longueur 8 ou 16 en hexa): ");
			message = SCANNER.nextLine().replaceAll(" ", "");
			System.out.println(message);
		}while(!(message.length() == 16 || message.length() == 32) || !message.matches("[0-9a-fA-F]+")); // 16 = 8*2 et 32 = 16 * 2 car deux char pour hex
		messageBytes = hexStringToByteArray(message);
		System.out.println("[*] Message : "+fromBytesToHexString(messageBytes));
		int length = DECRYPT_PREAMBULE.length + 1 + messageBytes.length + 1;
		byte[] decrypt_command = new byte[length];
		decrypt_command[DECRYPT_PREAMBULE.length] = (byte) messageBytes.length;
		java.lang.System.arraycopy(DECRYPT_PREAMBULE, 0, decrypt_command, 0, DECRYPT_PREAMBULE.length);
		java.lang.System.arraycopy(messageBytes, 0, decrypt_command, DECRYPT_PREAMBULE.length+1, messageBytes.length);
		decrypt_command[length-1] = MAX_LENGTH_MESSAGE;
		System.out.println(DELIMITEUR);
		String reponse = fromBytesToHexString(sendApdu(decrypt_command, "Envoi de la commande pour déchiffrer", cardChannel));
		System.out.println("[*] Message déchiffré           : "+reponse);
		System.out.println("[*] Message déchiffré en ascii  : "+hexToString(reponse));
		System.out.println(DELIMITEUR);
	}

	public static String fromBytesToHexString(byte inBytes[]) {
		StringBuffer buffer = new StringBuffer();
		for(int i=0;i<inBytes.length;i++) {
			buffer.append(Integer.toHexString((inBytes[i] & 0xF0)>>4).toUpperCase());
			buffer.append(Integer.toHexString(inBytes[i] & 0x0F).toUpperCase());
			buffer.append(" ");
		}
		return buffer.toString();
	}

	public static String hexToString(String hex){
		StringBuffer sb = new StringBuffer();
		for(int i = 0 ; i < hex.length()-1 ; i += 3)
			sb.append((char) Integer.parseInt(hex.substring(i, (i + 2)), 16));
		return sb.toString();
	}

	public static byte[] hexStringToByteArray(String s) {
		s = s.replaceAll(" ", "");
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	
	public static void banner(){
		System.out.println("#=====================================#");
		System.out.println("|         Projet Carte à puce         |");
		System.out.println("|      Chiffrement/Déchiffrement      |");
		System.out.println("|             Jordan Samhi            |");
		System.out.println("#=====================================#");
		System.out.println();
	}

}
