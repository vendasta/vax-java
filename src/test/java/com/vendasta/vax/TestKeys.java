package com.vendasta.vax;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

final class TestKeys {
    private TestKeys() {}

    static KeyPair ecKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    // mscli writes SEC1 "EC PRIVATE KEY" PEMs that include the public key, which the key parser needs
    static String sec1Pem(KeyPair keyPair) throws Exception {
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        org.bouncycastle.asn1.sec.ECPrivateKey privateKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(
            PrivateKeyInfo.getInstance(keyPair.getPrivate().getEncoded()).parsePrivateKey());
        org.bouncycastle.asn1.sec.ECPrivateKey sec1 = new org.bouncycastle.asn1.sec.ECPrivateKey(
            256, privateKey.getKey(), publicKeyInfo.getPublicKeyData(), publicKeyInfo.getAlgorithm().getParameters());
        StringWriter pem = new StringWriter();
        try (PemWriter writer = new PemWriter(pem)) {
            writer.writeObject(new PemObject("EC PRIVATE KEY", sec1.getEncoded()));
        }
        return pem.toString();
    }
}
