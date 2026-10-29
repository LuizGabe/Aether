package com.aether.api.service;

import static org.assertj.core.api.Assertions.*;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BackupCryptoTests {
    private final BackupCrypto crypto=new BackupCrypto();
    private final String password="A long test phrase - café 2026";
    @Test void roundTripIsPortableAndRandomized(){
        byte[] data="Private financial test data".getBytes(StandardCharsets.UTF_8);
        byte[] first=crypto.encrypt(data,password), second=crypto.encrypt(data,password);
        assertThat(first).isNotEqualTo(second);
        assertThat(new String(first,0,8,StandardCharsets.US_ASCII)).isEqualTo("AETHBK01");
        assertThat(new BackupCrypto().decrypt(first,password)).isEqualTo(data);
        assertThat(first.length).isEqualTo(data.length+56);
        assertThat(new String(first,StandardCharsets.ISO_8859_1)).doesNotContain("Private financial test data");
    }
    @Test void authenticatesSaltNonceCiphertextAndTag(){
        byte[] archive=crypto.encrypt("data".getBytes(StandardCharsets.UTF_8),password);
        for(int offset:new int[]{12,28,40,archive.length-1}){
            byte[] altered=archive.clone();altered[offset]^=1;
            assertThatThrownBy(()->crypto.decrypt(altered,password)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(()->crypto.decrypt(archive,"different password 2026")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsUnsupportedParametersAndTruncationBeforeDecryption(){
        byte[] archive=crypto.encrypt(new byte[32],password);
        ByteBuffer.wrap(archive).putInt(8,Integer.MAX_VALUE);
        assertThatThrownBy(()->crypto.decrypt(archive,password)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->crypto.decrypt(Arrays.copyOf(archive,20),password)).isInstanceOf(IllegalArgumentException.class);
        archive[0]^=1;
        assertThatThrownBy(()->crypto.decrypt(archive,password)).isInstanceOf(IllegalArgumentException.class);
    }
}
