package com.kakaoscan.profile;

import com.kakaoscan.profile.domain.socket.client.NettyClient;
import com.kakaoscan.profile.domain.socket.client.NettyClientInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@RequiredArgsConstructor
@PropertySource({"classpath:application-db.properties",
		  		 "classpath:application-key.properties",
		  		 "classpath:application-link.properties",
		  		 "classpath:application-smtp.properties",
		  		 "classpath:application-redis.properties",
		  		 "classpath:application-kafka.properties",
		  		 "classpath:application-messagebot.properties",
		 		 "classpath:application-oauth.yml"})
public class ProfileApplication {
	@Value("${tcp.host}")
	private String[] hosts;

	private final NettyClientInstance nettyClientInstance;
	private final NettyClient nettyClient;

	public static void main(String[] args) {
		SpringApplication.run(ProfileApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void connectNetty(){
		for (int i = 0; i < nettyClientInstance.getBootstrap().length; i++) {
			nettyClient.connect(nettyClientInstance.getBootstrap()[i], hosts[i]);
		}
	}
}
