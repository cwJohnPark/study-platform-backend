package com.study.platform.auth;

import java.util.Collections;

import javax.servlet.http.HttpSession;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.study.platform.member.domain.Member;
import com.study.platform.member.MemberRepository;
import com.study.platform.auth.dto.OAuthAttributes;
import com.study.platform.auth.session.SessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
	private final MemberRepository memberRepository;
	private final HttpSession httpSession;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
		OAuth2User oAuth2User = delegate.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		String userNameAttributeName = userRequest.getClientRegistration()
			.getProviderDetails()
			.getUserInfoEndpoint()
			.getUserNameAttributeName();

		OAuthAttributes attributes = OAuthAttributes
			.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

		Member member = saveOrUpdate(attributes);

		httpSession.setAttribute("user", new SessionUser(member));

		return new DefaultOAuth2User(
			Collections.singleton(new SimpleGrantedAuthority(member.getDeveloperType().name())),
				attributes.getAttributes(),
				attributes.getNameAttributeKey());
	}

	private Member saveOrUpdate(OAuthAttributes attributes) {
		Member member = memberRepository.findMemberByGithubId(attributes.getGithubId())
			.orElse(attributes.toEntity());
		return memberRepository.save(member);
	}
}
