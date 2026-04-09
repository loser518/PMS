package com.project.pms.config;

import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class Ip2RegionConfig {
    
    @Bean
    @Qualifier("ipv4Searcher")
    public Searcher ipv4Searcher() throws IOException {
        Resource resource = new ClassPathResource("ip2region_v4.xdb");
        byte[] cBuff = Searcher.loadContentFromFile(resource.getFile().getPath());
        return Searcher.newWithBuffer(cBuff);
    }
    
    @Bean
    @Qualifier("ipv6Searcher")
    public Searcher ipv6Searcher() throws IOException {
        Resource resource = new ClassPathResource("ip2region_v6.xdb");
        byte[] cBuff = Searcher.loadContentFromFile(resource.getFile().getPath());
        return Searcher.newWithBuffer(cBuff);
    }
}