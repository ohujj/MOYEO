package com.moyeo.service.meeting;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class InMemoryCommercialAreaCatalog implements CommercialAreaCatalog {

    private static final List<CommercialArea> AREAS = List.of(
            area("1001491", "삼각지역", "관광특구", "37.5344715", "126.9726696", "용산구", "한강로동"),
            area("1001492", "남영동 먹자골목", "관광특구", "37.5420575", "126.9733574", "용산구", "남영동"),
            area("1001494", "이태원(이태원역)", "관광특구", "37.5344240", "126.9901623", "용산구", "이태원1동"),
            area("1001495", "망원역", "관광특구", "37.5566673", "126.9100025", "마포구", "망원제1동"),
            area("1001496", "합정역", "관광특구", "37.5499229", "126.9121466", "마포구", "합정동"),
            area("1001497", "녹두거리(대학동)", "관광특구", "37.4704910", "126.9365294", "관악구", "대학동"),
            area("1001498", "한남오거리", "관광특구", "37.5332007", "127.0078941", "용산구", "한남동"),
            area("1001499", "종각역", "관광특구", "37.5701802", "126.9831145", "종로구", "종로1.2.3.4가동"),
            area("1001500", "강남역", "관광특구", "37.4979521", "127.0276194", "강남구", "역삼1동"),
            area("1001501", "홍대입구역", "관광특구", "37.5571926", "126.9253811", "마포구", "서교동")
    );

    @Override
    public List<CommercialArea> findAll() {
        return AREAS;
    }

    private static CommercialArea area(
            String areaCode,
            String areaName,
            String categoryName,
            String latitude,
            String longitude,
            String guName,
            String dongName
    ) {
        return new CommercialArea(
                areaCode,
                areaName,
                categoryName,
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                guName,
                dongName
        );
    }
}
