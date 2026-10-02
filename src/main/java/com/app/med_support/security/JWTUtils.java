package com.app.med_support.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtils {
    //CHEKE THE TOKEN AND MAKE SURE
    @Value("${jwt.secret}")
    private String jwtSecret;
// TOKEN EXPAIRY PERIOD
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    // THE METHOD GET THE STRUNG EMAIL CAUSE WE NEED TO KNOW THE TOKEN IS RELETED TO WHO
    public String generateJwtToken(String email) {
        // WHEN THE TOKEN IS CREATED
        Date now = new Date();
        //WHEN IT WILL EXPIRY DATE (24H) = jwt.expiration=86400000
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        // JWT.BUILDER -- CREATE NEW JWT
        // SUBJECT ( --) -- THE TOKEN IS FOR WHO THROW THE FOR EXAMPLE EMAIL
        // SINGWITH -- SING THE JWT WITH THE SINGKEY SO THE SERVER CAN DETECT IF THE TOKEN WAS MODIFIED
        // COMPACT -- BUILD THE FINAL JWT AND RETURN IT AS A STRING
        return Jwts.builder().subject(email).issuedAt(now).expiration(expiryDate)
        .signWith(getSigningKey()).compact();
    }
    public String getEmailFromJwtToken(String token) {
        // parser -- is for reading the JWT we used a BUILDER to build new JWT
        //verifywith -- we go back for the signature when we create the JWT we used SINGWITH
        // BUILD HERE IS FOR AFTER MAKING THE PARSER BULIT IT AND KEEP IT READY TO USE
        //.parseSignedClaims(token)-- GIVE THE PARSER THE REAL JWT AND VERIFY THAT SIGNATURE IS VALID
        //GET PAYLOAD-- SHOWS THE USER INFORMATION THAT CONTAINS CLAIMS
        // GET SUBJECT -- GIVE ME THE CLAIMS FROM THE SUBJECT WE USED THE (EMAIL) IN THE GENERATION SUBJECT

        return Jwts.parser().verifyWith(getSigningKey()).build()
        .parseSignedClaims(token).getPayload().getSubject();
    }
}
