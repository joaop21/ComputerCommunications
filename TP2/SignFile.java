import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class SignFile{
	private List<byte[]> list;

	//The constructor of SignFile class builds the list that will be written to the file.
	//The list consists of the File and the signature.
	public SignFile(File f, String keyFile) throws InvalidKeyException, Exception {
		list = new ArrayList<byte[]>();
		list.add(Files.readAllBytes(f.toPath()));
		list.add(sign(f, keyFile));
	}

	//The method that signs the data using the private key that is stored in keyFile path
	public byte[] sign(File f, String keyFile) throws InvalidKeyException, Exception{
		Signature rsa = Signature.getInstance("SHA1withRSA");
		rsa.initSign(getPrivate(keyFile));
		rsa.update(Files.readAllBytes(f.toPath()));
		return rsa.sign();
	}

	//Method to retrieve the Private Key from a file
	public PrivateKey getPrivate(String filename) throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	//Method to write the List of byte[] to a file
	private void writeToFile(String filename) throws FileNotFoundException, IOException {
		File f = new File(filename);
		f.getParentFile().mkdirs();
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
	    out.writeObject(list);
		out.close();
	}

    public static void generatesignedFile(File f, String outFile) throws InvalidKeyException, IOException, Exception{
        new SignFile(f, "KeyPair/privateKey").writeToFile(outFile);
    }
}
