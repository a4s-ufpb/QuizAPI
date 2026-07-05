package br.ufpb.dcx.apps4society.quizapi.repository;

import br.ufpb.dcx.apps4society.quizapi.entity.User;

public interface GlobalRankingProjection {
    User getUser();
    Double getTotal();
}
