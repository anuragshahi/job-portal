package com.example.bff.util;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${bff.jwt.signing-key}")
    private String signingKey;

    @Value("${bff.jwt.issuer}")
    private String issuer;

    @Value("${bff.session.ttl-minutes}")
    private int sessionTtlMinutes;

    public String issueSessionJwt(String jti, OAuth2AuthenticationToken auth) {
        try {
            OidcUser user = (OidcUser) auth.getPrincipal();

            Date now = new Date();
            Date exp = new Date(now.getTime() + (long) sessionTtlMinutes * 60 * 1000);

            assert user != null;
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getName())
                    .jwtID(jti)
                    .issueTime(now)
                    .expirationTime(exp)
                    .claim("email", user.getEmail())
                    .claim("name", user.getFullName())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),
                    claims);

            signedJWT.sign(new RSASSASigner(loadPrivateKey(signingKey)));

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to issue JWT", e);
        }
    }

    public String extractJti(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // 1. Verify Signature
            PrivateKey privateKey = loadPrivateKey(signingKey);
            RSAPublicKey publicKey = derivePublicKey((RSAPrivateCrtKey) privateKey);
            
            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return null; // Invalid signature
            }

            // 2. Validate Expiration
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || new Date().after(expirationTime)) {
                return null; // Expired
            }

            // 3. Validate Issuer
            String claimIssuer = signedJWT.getJWTClaimsSet().getIssuer();
            if (!issuer.equals(claimIssuer)) {
                return null; // Invalid Issuer
            }

            return signedJWT.getJWTClaimsSet().getJWTID();
        } catch (Exception e) {
            return null;
        }
    }

    private PrivateKey loadPrivateKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private RSAPublicKey derivePublicKey(RSAPrivateCrtKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
    }
}
