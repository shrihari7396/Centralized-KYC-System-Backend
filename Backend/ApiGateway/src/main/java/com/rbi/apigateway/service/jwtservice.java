//package com.rbi.apigateway.service;
//
//import io.jsonwebtoken.io.Decoders;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.security.Key;
//import java.util.Date;
//
//@Service
//public class jwtservice {
//    @Value("${jwt.token}")
//    private String secretkey;
//
//    private Key getkey(){
//        byte[] keyBytes  =  secretkey.getBytes();
//        return Keys.hmacShaKeyFor(secretkey.getBytes());
////    }
//    Boolean validateToken(String token){
//        return istokenExpired(token);
//    }
//    Boolean istokenExpired(String token){
//        extracttokenexpiry(token).before(new Date());
//    }
//    Date extracttokenexpiry(String token){
//        extractclaims(token)
//    }
//}
