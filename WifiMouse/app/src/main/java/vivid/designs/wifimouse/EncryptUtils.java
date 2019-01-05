package vivid.designs.wifimouse;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtils {
    private static Cipher AESCipher = null;
    private static void initCipher(byte[] key, byte[] iv, int opmode) {
        if(AESCipher == null) {
            try { AESCipher = Cipher.getInstance("AES/CBC/NoPadding"); }
            catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
            catch (NoSuchPaddingException e) { e.printStackTrace(); }
        }

        SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        try { AESCipher.init(opmode, secretKey, ivSpec); }
        catch (InvalidKeyException e) { e.printStackTrace(); }
        catch (InvalidAlgorithmParameterException e) { e.printStackTrace(); }
    }

    public static byte[] encryptBytesAES(byte[] bytes, byte[] key, byte[] iv) {
        initCipher(key, iv, Cipher.ENCRYPT_MODE);
        try {
            return AESCipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // got illegal state exception here and get some segfaults..
        }
        return new byte[0];
    }

    public static byte[] decryptBytesAES(byte[] bytes, byte[] key, byte[] iv) {
        initCipher(key, iv, Cipher.DECRYPT_MODE);
        try {
            return AESCipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private static MessageDigest sha256 = null;
    public static byte[] makeHash16(int num) { return makeHash16(Integer.toString(num)); }
    public static byte[] makeHash16(String str) {
        if(sha256 == null) {
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        byte[] bytes = new byte[16];
        try { bytes = str.getBytes("UTF-8"); }
        catch (UnsupportedEncodingException e) { e.printStackTrace(); }
        byte[] hash = sha256.digest(bytes);

        byte[] sized16 = new byte[16];
        System.arraycopy(hash, 0, sized16, 0, 16);
        return sized16;
    }
}
