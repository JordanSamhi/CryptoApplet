package sm.appli;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class appGUI extends JFrame{

	private static final long serialVersionUID = 1L;

	public static final byte[] SELECT_APPLET = {(byte)0x00,(byte)0xa4,(byte)0x04,(byte)0x00,(byte)0x07,(byte)0xee,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xbb,(byte)0x02};
	public static byte[] SEND_KEY = {(byte)0x90,(byte)0x10,(byte)0x00,(byte)0x00,(byte)0x08,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x10};
	public static final byte[] ENCRYPT_PREAMBULE = {(byte)0x90,(byte)0x20,(byte)0x00,(byte)0x00};
	public static final byte[] DECRYPT_PREAMBULE = {(byte)0x90,(byte)0x30,(byte)0x00,(byte)0x00};
	public static final byte MAX_LENGTH_MESSAGE = 0x10;

	public CardChannel cardChannel;

	private JPanel container = new JPanel();
	private JTextField champCle = new JTextField();
	private JLabel labelCle = new JLabel("Entrez la clé : ");
	private JButton btnValiderCle = new JButton ("Valider");
	private JLabel atrLabel = new JLabel();
	private JLabel atrLabelVal = new JLabel();
	private JLabel channelLabel = new JLabel();
	private JLabel carteInsere = new JLabel();
	private JLabel champErreurCle = new JLabel();

	private JLabel cleAscii = new JLabel();
	private JLabel cleBytes = new JLabel();
	private JLabel labelMessage = new JLabel("Message : ");
	private JTextField champMessage = new JTextField();
	private JButton btnChiffrer = new JButton ("Chiffrer");
	private JButton btnDechiffrer = new JButton ("Déchiffrer");
	private JLabel erreursMessage = new JLabel();
	
	public static FenetreLogs FENETRELOGS;

	public appGUI() throws CardException{
		FENETRELOGS = new FenetreLogs();

		this.initCard();

		this.setTitle("Application Carte à puce");
		this.setSize(800, 300);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setLocation(100, 300);
		
		this.container.setBackground(Color.white);
		this.container.setLayout(new BorderLayout());

		JPanel top = new JPanel(new GridBagLayout());      

		Font police = new Font("Arial", Font.BOLD, 14);
		this.champCle.setFont(police);
		this.champCle.setPreferredSize(new Dimension(150, 30));
		this.champCle.setForeground(Color.BLACK);
		this.champMessage.setFont(police);
		this.champMessage.setPreferredSize(new Dimension(400, 30));
		this.champMessage.setMinimumSize(this.champMessage.getPreferredSize());
		this.champMessage.setForeground(Color.BLACK);


		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;

		top.add(this.carteInsere, c);
		c.gridy = 1;
		top.add(this.atrLabel, c);
		c.gridy = 2;
		top.add(this.atrLabelVal, c);
		c.gridy = 3;
		top.add(this.channelLabel, c);
		c.gridy = 4;
		top.add(this.labelCle, c);
		c.gridx = 1;
		top.add(this.champCle, c);
		c.gridx = 2;
		top.add(this.btnValiderCle, c);
		c.gridx = 0;
		c.gridy = 5;
		top.add(this.champErreurCle, c);


		// Placement 2eme fenetre
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		top.add(cleAscii, c);
		this.cleAscii.setVisible(false);
		c.gridy = 1;
		top.add(cleBytes, c);
		this.cleBytes.setVisible(false);
		c.gridy = 2;
		top.add(this.labelMessage, c);
		this.labelMessage.setVisible(false);
		c.gridx = 1;
		top.add(this.champMessage, c);
		this.champMessage.setVisible(false);
		c.gridx = 2;
		top.add(this.btnChiffrer, c);
		this.btnChiffrer.setVisible(false);
		c.gridx = 3;
		top.add(this.btnDechiffrer, c);
		this.btnDechiffrer.setVisible(false);
		c.gridy = 3;
		c.gridx = 1;
		top.add(this.erreursMessage, c);
		this.erreursMessage.setVisible(false);

		this.btnValiderCle.addActionListener(new boutonValiderCle());
		this.btnChiffrer.addActionListener(new boutonChiffrer());
		this.btnDechiffrer.addActionListener(new boutonDechiffrer());

		this.setContentPane(top);
		this.setVisible(true);         
	}

	public class boutonChiffrer implements ActionListener{
		public void actionPerformed(ActionEvent e){
			String message = champMessage.getText();
			byte[] messageBytes = null;
			if(message.length() <= 0 || message.length() > 16){
				erreursMessage.setForeground(Color.RED);
				erreursMessage.setText("La longueur du message doit être entre 1 et 16 caractères");
			}
			else{
				erreursMessage.setForeground(Color.BLACK);
				erreursMessage.setText("");
				messageBytes = message.getBytes();

				int length = ENCRYPT_PREAMBULE.length + 1 + messageBytes.length + 1;
				byte[] encrypt_command = new byte[length];
				encrypt_command[ENCRYPT_PREAMBULE.length] = (byte) messageBytes.length;
				java.lang.System.arraycopy(ENCRYPT_PREAMBULE, 0, encrypt_command, 0, ENCRYPT_PREAMBULE.length);
				java.lang.System.arraycopy(messageBytes, 0, encrypt_command, ENCRYPT_PREAMBULE.length+1, messageBytes.length);
				encrypt_command[length-1] = MAX_LENGTH_MESSAGE;
				String reponse = fromBytesToHexString(sendApdu(encrypt_command, cardChannel));
				erreursMessage.setText("<html>Message Original : "+message+"<br />"+"Message chiffré : "+reponse+"<br />"+"Message chiffré en ascii : "+hexToString(reponse)+"</html>");
			}
		}
	}

	public class boutonDechiffrer implements ActionListener{
		public void actionPerformed(ActionEvent e){
			String message = champMessage.getText().replaceAll(" ", "");
			byte[] messageBytes = null;
			if(!(message.length() == 16 || message.length() == 32) || !message.matches("[0-9a-fA-F]+")){ // 16 = 8*2 et 32 = 16 * 2 car deux char pour hex
				erreursMessage.setForeground(Color.RED);
				erreursMessage.setText("La longueur doit être de 8 ou 16 caractères en hexadécimal)");
			}else{
				erreursMessage.setForeground(Color.BLACK);
				erreursMessage.setText("");
				messageBytes = hexStringToByteArray(message);
				int length = DECRYPT_PREAMBULE.length + 1 + messageBytes.length + 1;
				byte[] decrypt_command = new byte[length];
				decrypt_command[DECRYPT_PREAMBULE.length] = (byte) messageBytes.length;
				java.lang.System.arraycopy(DECRYPT_PREAMBULE, 0, decrypt_command, 0, DECRYPT_PREAMBULE.length);
				java.lang.System.arraycopy(messageBytes, 0, decrypt_command, DECRYPT_PREAMBULE.length+1, messageBytes.length);
				decrypt_command[length-1] = MAX_LENGTH_MESSAGE;
				String reponse = fromBytesToHexString(sendApdu(decrypt_command, cardChannel));
				erreursMessage.setText("<html>Message Original : "+message+"<br />"+"Message déchiffré : "+reponse+"<br />"+"Message déchiffré en ascii : "+hexToString(reponse)+"</html>");
			}
		}
	}

	public class boutonValiderCle implements ActionListener{
		public void actionPerformed(ActionEvent e){
			String champ = champCle.getText();
			if(champ.length() != 8)
				champErreurCle.setText("La clé doit faire exactement 8 caractères");
			else{
				String cle = champCle.getText();
				byte[] cleOctets = cle.getBytes();
				java.lang.System.arraycopy(cleOctets, 0, SEND_KEY, 5, 8);

				cleAscii.setText("Clé : "+cle);
				cleBytes.setText("Clé en Bytes : "+fromBytesToHexString(cleOctets));

				sendApdu(SELECT_APPLET, cardChannel);

				sendApdu(SEND_KEY, cardChannel);

				cacherFenetre1();
				montrerFenetre2();

			}
		}
	}
	
	public class FenetreLogs extends JFrame{
		private static final long serialVersionUID = 1L;
		public JPanel top = new JPanel(new GridBagLayout());
		public JLabel logs = new JLabel();
		public static final String HTMLDEBUT = "<html>";
		public static final String HTMLFIN = "</html>";
		public String logsString = "";
		public FenetreLogs(){
			this.setTitle("Logs");
			this.setSize(600, 800);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.setLocationRelativeTo(null);
			this.setLocation(1000, 100);
			this.top.add(this.logs);
			this.setContentPane(this.top);
			this.setVisible(true);
		}
		public void addLog(String log){
			this.logsString = this.logsString + "<br />" + log;
			this.logs.setText(HTMLDEBUT+this.logsString+HTMLFIN);
		}
		
	}

	public void cacherFenetre1(){
		carteInsere.setVisible(false);
		atrLabel.setVisible(false);
		atrLabelVal.setVisible(false);
		channelLabel.setVisible(false);
		labelCle.setVisible(false);
		champCle.setVisible(false);
		btnValiderCle.setVisible(false);
		champErreurCle.setVisible(false);
	}

	public void montrerFenetre2(){
		cleAscii.setVisible(true);
		cleBytes.setVisible(true);
		labelMessage.setVisible(true);
		champMessage.setVisible(true);
		btnChiffrer.setVisible(true);
		btnDechiffrer.setVisible(true);
		erreursMessage.setVisible(true);
	}


	public void initCard() throws CardException {
		TerminalFactory defaultTermFactory = TerminalFactory.getDefault();
		CardTerminals terminalList = defaultTermFactory.terminals();
		CardTerminal cardTerminal = (CardTerminal) terminalList.list().get(0);
		cardTerminal.waitForCardPresent(15);

		if(cardTerminal.isCardPresent()){
			this.carteInsere.setForeground(Color.GREEN);
			this.carteInsere.setText("Carte insérée");
			String CONNECTION_PROTOCOL = "*";
			Card card = cardTerminal.connect(CONNECTION_PROTOCOL);

			ATR cardATR = card.getATR();
			byte[] atrBytes = cardATR.getBytes();
			String atrHex = fromBytesToHexString(atrBytes);
			this.atrLabel.setText(cardATR.toString());
			this.atrLabelVal.setText("Valeur ATR : "+atrHex);

			cardChannel = card.getBasicChannel();
			this.channelLabel.setText("Channel : "+String.valueOf(cardChannel));

		}
		else{
			this.carteInsere.setForeground(Color.RED);
			this.carteInsere.setText("Carte non insérée");
			labelCle.setVisible(false);
			btnValiderCle.setVisible(false);
			champCle.setVisible(false);
		}
	}

	public static byte[] sendApdu(byte[] command, CardChannel cardChannel){
		CommandAPDU apdu = new CommandAPDU(command);
		ResponseAPDU reponse = null;
		try {
			reponse = cardChannel.transmit(apdu);
		} catch (CardException e) {
			System.out.println(e);
		}
		FENETRELOGS.addLog("--&gt; "+fromBytesToHexString(apdu.getBytes()));
		FENETRELOGS.addLog("&lt;-- "+fromBytesToHexString(reponse.getBytes()));
		return reponse.getData();
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

	public static void main(String[] args) {
		try {
			new appGUI();
		} catch (CardException e) {
			System.exit(1);
		}
	}

}
