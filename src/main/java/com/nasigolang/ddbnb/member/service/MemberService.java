package com.nasigolang.ddbnb.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nasigolang.ddbnb.login.dto.RenewTokenDTO;
import com.nasigolang.ddbnb.member.dto.MemberDTO;
import com.nasigolang.ddbnb.member.dto.MemberSimpleDTO;
import com.nasigolang.ddbnb.member.entity.Member;
import com.nasigolang.ddbnb.member.repository.MemberRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Date;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public MemberService(MemberRepository memberRepository, ModelMapper modelMapper, ObjectMapper objectMapper) {
        this.memberRepository = memberRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public long registNewUser(MemberDTO newMember) {
        System.out.println(9);
        newMember.setNickname("새로운회원" + (Math.random() * 100 + 1));
        System.out.println(10);
        return memberRepository.save(modelMapper.map(newMember, Member.class)).getMemberId();
    }

    public MemberDTO findMemberById(long memberId) {

        Member member = memberRepository.findById(memberId).get();

        return modelMapper.map(member, MemberDTO.class);
    }


    public Page<MemberSimpleDTO> findAllMembers(Pageable pageable) {

        pageable = PageRequest.of(pageable.getPageNumber() <= 0 ? 0 : pageable.getPageNumber() - 1, pageable.getPageSize(), Sort.by("memberId"));

        return memberRepository.findAll(pageable).map(member -> modelMapper.map(member, MemberSimpleDTO.class));
    }

    @Transactional
    public void modifyMember(MemberDTO modifyInfo, long memberId, String type) {

        Member foundMember = memberRepository.findById(memberId).get();

        switch (type) {
            case "edit":
                if (modifyInfo.getNickname().length() > 0) {
                    foundMember.setNickname(modifyInfo.getNickname());
                }
                break;

            case "deactivate":

                RestTemplate rt = new RestTemplate();

                /* 카카오 로그인일 때 */
                if (foundMember.getSocialLogin().equals("KAKAO")) {

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Authorization", "Bearer " + foundMember.getAccessToken());

                    HttpEntity<MultiValueMap<String, String>> kakaoDeactivateRequest = new HttpEntity<>(headers);

                    ResponseEntity<String> kakaoDeactivateResponse = rt.exchange("https://kapi.kakao.com/v1/user/unlink", HttpMethod.POST, kakaoDeactivateRequest, String.class);

                    String kakaoDeactivateResult = "";

                    try {
                        kakaoDeactivateResult = objectMapper.readValue(kakaoDeactivateResponse.getBody(), String.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    foundMember.setStatus("Y");
                    break;

                    /* 네이버 로그인일 때 */
                } else if (foundMember.getSocialLogin().equals("NAVER")) {

                    /* 갱신 먼저 */
                    Date expireDate = new Date(foundMember.getAccessTokenExpireDate());

                    if (expireDate.before(new Date())) {

                        RestTemplate rtForRenew = new RestTemplate();

                        HttpHeaders headers = new HttpHeaders();

                        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                        params.add("client_id", System.getenv("NaverClientIdKey"));
                        params.add("client_secret", System.getenv("NaverClientSecretKey"));
                        params.add("refresh_token", foundMember.getRefreshToken());
                        params.add("grant_type", "refresh_token");

                        HttpEntity<MultiValueMap<String, String>> naverRenewRequest = new HttpEntity<>(params, headers);

                        ResponseEntity<String> naverRenewResponses = rtForRenew.exchange("https://nid.naver.com/oauth2.0/token", HttpMethod.GET, naverRenewRequest, String.class);

                        ObjectMapper objectMapper = new ObjectMapper();
                        RenewTokenDTO renewToken = null;
                        try {
                            renewToken = objectMapper.readValue(naverRenewResponses.getBody(), RenewTokenDTO.class);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        if (renewToken.getRefresh_token() != null) {

                            foundMember.setRefreshToken(renewToken.getRefresh_token());
                            foundMember.setRefreshTokenExpireDate((1000 * 60 * 60 * 6) + System.currentTimeMillis());
                        }

                        foundMember.setAccessToken(renewToken.getAccess_token());
                        foundMember.setAccessTokenExpireDate(renewToken.getExpires_in() + System.currentTimeMillis());

                    }

                    /* 업데이트 된 멤버 다시 가져옴 */
                    foundMember = memberRepository.findById(memberId).get();

                    /* 탈퇴 요청 */
                    HttpHeaders headers = new HttpHeaders();

                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("client_id", System.getenv("NaverClientIDKey"));
                    params.add("client_secret", System.getenv("NaverClientSecretKey"));
                    params.add("access_token", foundMember.getAccessToken());
                    params.add("grant_type", "delete");

                    HttpEntity<MultiValueMap<String, String>> naverDeactivateRequest = new HttpEntity<>(headers, params);

                    ResponseEntity<String> naverDeactivateResponse = rt.exchange("https://nid.naver.com/oauth2.0/token", HttpMethod.POST, naverDeactivateRequest, String.class);

                    System.out.println(naverDeactivateResponse.getBody());

                    String naverDeactivateResult = "";

                    try {
                        naverDeactivateResult = objectMapper.readValue(naverDeactivateResponse.getBody(), String.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    foundMember.setStatus("Y");
                    break;
                }
        }
    }

    public MemberDTO findBySocialId(String socialLogin, String socialId) {
        System.out.println(5);
        Member foundMember = memberRepository.findBySocialId(socialLogin, socialId);
        System.out.println(6);
        if (foundMember == null) {
            return null;
        } else {
            return modelMapper.map(foundMember, MemberDTO.class);
        }
    }

    public int findMemberAmount() {
        return memberRepository.findMemberAmount();
    }

    public int findMemberVisitant(LocalDate now) {
        
        return memberRepository.findByLastVisitDate(now);
    }

    public Page<MemberSimpleDTO> findMemberBySignDayIsToday(Pageable page, LocalDate now) {

        page = PageRequest.of(page.getPageNumber() <= 0 ? 0 : page.getPageNumber() - 1, 5, Sort.by("signDate"));

        return memberRepository.findMemberBySignDate(page, now).map(member -> modelMapper.map(member, MemberSimpleDTO.class));

    }
}