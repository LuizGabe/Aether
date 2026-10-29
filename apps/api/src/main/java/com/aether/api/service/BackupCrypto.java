package com.aether.api.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.stereotype.Component;

/** Portable password-protected archives; no password or derived key is persisted. */
@Component
public class BackupCrypto {
    private static final byte[] MAGIC="AETHBK01".getBytes(StandardCharsets.US_ASCII);
    private static final int ITERATIONS=600_000, HEADER_SIZE=40, TAG_SIZE=16;
    public static final int MAX_PLAINTEXT=100*1024*1024, MAX_ARCHIVE=MAX_PLAINTEXT+HEADER_SIZE+TAG_SIZE;
    private final SecureRandom random=new SecureRandom();
    public void validatePassword(String password){
        if(password==null||password.length()<12||password.length()>1024||password.isBlank())
            throw new IllegalArgumentException("Use uma senha de 12 a 1024 caracteres para proteger o backup.");
    }
    public boolean encrypted(byte[] content){return content.length>=MAGIC.length&&Arrays.equals(MAGIC,Arrays.copyOf(content,MAGIC.length));}
    public byte[] encrypt(byte[] plaintext,String password){
        validatePassword(password);
        if(plaintext.length>MAX_PLAINTEXT)throw new IllegalArgumentException("O histórico excede o limite de 100 MB por backup.");
        byte[] salt=new byte[16],iv=new byte[12];random.nextBytes(salt);random.nextBytes(iv);
        byte[] header=ByteBuffer.allocate(HEADER_SIZE).put(MAGIC).putInt(ITERATIONS).put(salt).put(iv).array();
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),new GCMParameterSpec(128,iv));cipher.updateAAD(header);
            byte[] ciphertext=cipher.doFinal(plaintext);
            return ByteBuffer.allocate(header.length+ciphertext.length).put(header).put(ciphertext).array();
        }catch(GeneralSecurityException e){throw new IllegalStateException("Não foi possível criptografar o backup local.");}
    }
    public byte[] decrypt(byte[] archive,String password){
        validatePassword(password);
        if(archive.length<HEADER_SIZE+TAG_SIZE||archive.length>MAX_ARCHIVE||!encrypted(archive))
            throw new IllegalArgumentException("Formato de backup criptografado inválido ou incompatível.");
        ByteBuffer input=ByteBuffer.wrap(archive);input.position(MAGIC.length);
        if(input.getInt()!=ITERATIONS)throw new IllegalArgumentException("Parâmetros do backup inválidos ou incompatíveis.");
        byte[] salt=new byte[16],iv=new byte[12];input.get(salt);input.get(iv);
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(password,salt),new GCMParameterSpec(128,iv));
            cipher.updateAAD(Arrays.copyOf(archive,HEADER_SIZE));
            return cipher.doFinal(archive,HEADER_SIZE,archive.length-HEADER_SIZE);
        }catch(GeneralSecurityException e){throw new IllegalArgumentException("Senha incorreta ou backup danificado. Os dados atuais não foram alterados.");}
    }
    private SecretKey key(String password,byte[] salt)throws GeneralSecurityException{
        char[] chars=password.toCharArray();
        PBEKeySpec spec=new PBEKeySpec(chars,salt,ITERATIONS,256);Arrays.fill(chars,'\0');
        byte[] derived=null;
        try {derived=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();return new SecretKeySpec(derived,"AES");}
        finally{spec.clearPassword();if(derived!=null)Arrays.fill(derived,(byte)0);}
    }
}
