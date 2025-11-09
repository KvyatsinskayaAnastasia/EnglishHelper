package com.learn.english.mapper;

import com.learn.english.entity.WordEO;
import com.learn.english.model.WordState;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WordMapper {
    WordEO toEO(WordState wordState, Long userId);
}

