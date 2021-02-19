package com.example.demo;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(method = RequestMethod.GET)
public class AccountController {
    final String client_id = "client_id";
    final String client_secret = "client_secret";
    final String redirect_uri = "redirect_uri";

    @RequestMapping(value = "/callNaverLogin")
    public ResponseEntity<?> test1(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // naver login test

        // 세션 또는 별도의 저장 공간에 상태 토큰을 저장
        String state = generateState();
        HttpSession session = request.getSession();
        session.setAttribute("state", state);
        /*
         * https://nid.naver.com/oauth2.0/authorize?client_id={클라이언트아이디}&response_type=
         * code&redirect_uri={개발자 센터에 등록한 콜백 URL(URL 인코딩)}&state={상태 토큰}
         */

        String redirectUri = URLEncoder.encode(redirect_uri, "UTF-8");

        String url = "https://nid.naver.com/oauth2.0/authorize?client_id=" + client_id
                + "&response_type=code&redirect_uri=" + redirectUri + "&state=" + state;
        System.out.println(url);

        HashMap<String, Object> data = new HashMap<>();
        data.put("data", url);

        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    @RequestMapping(value = "/res")
    public void res(HttpServletRequest request, HttpServletResponse response, String code, String state)
            throws IOException {
        HttpSession session = request.getSession();
        System.out.println(session.getAttribute("state"));

        String url = "https://nid.naver.com/oauth2.0/token?client_id=" + client_id + "&client_secret=" + client_secret
                + "&grant_type=authorization_code&state=" + state + "&code=" + code;
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 타임아웃 설정 5초
        factory.setReadTimeout(5000);// 타임아웃 설정 5초
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders header = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(header);

        UriComponents uri = UriComponentsBuilder.fromHttpUrl(url).build();
        ResponseEntity<HashMap> rst = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, HashMap.class);

        Map<String, Object> resStr = rst.getBody();
        // Entries obj = mapper.readValue(resStr, Entries.class);
        String profile = getProfile(resStr);
        System.out.println(resStr.get("access_token"));
        session.setAttribute("token", resStr);

        response.sendRedirect("http://localhost:8082?profile=" + profile);
    }

    public String getProfile(Map<String, Object> res) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 타임아웃 설정 5초
        factory.setReadTimeout(5000);// 타임아웃 설정 5초
        RestTemplate restTemplate = new RestTemplate(factory);
        HttpHeaders header = new HttpHeaders();
        header.add("Authorization", res.get("token_type") + " " + res.get("access_token"));
        HttpEntity<?> entity = new HttpEntity<>(header);
        String url = "https://openapi.naver.com/v1/nid/me";
        UriComponents uri = UriComponentsBuilder.fromHttpUrl(url).build();
        ResponseEntity<HashMap> rst = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, HashMap.class);
        System.out.println(rst.getBody().get("response"));
        return rst.getBody().toString();
    }

    public String generateState() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }
}
