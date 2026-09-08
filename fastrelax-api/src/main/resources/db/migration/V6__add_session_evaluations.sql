-- =========================================================================
-- AVALIAÇÃO DA MASSAGEM
--
-- Uma nota por sessão concluída, dada pelo próprio colaborador logo depois da
-- massagem. O company_id é redundante em relação ao da sessão de propósito: é
-- ele que permite o RH filtrar sem passar por join, e é o que o predicado de
-- tenant do CompanyScopedRepository procura em toda consulta.
-- =========================================================================
BEGIN;

CREATE TABLE session_evaluations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    collaborator_id BIGINT NOT NULL REFERENCES collaborators(id),

    -- ON DELETE CASCADE: sessão é registro histórico e não é apagada, mas se um
    -- dia for, a avaliação órfã não teria sentido nenhum.
    session_id BIGINT NOT NULL UNIQUE REFERENCES collaborator_sessions(id) ON DELETE CASCADE,

    score INT NOT NULL CHECK (score BETWEEN 1 AND 5),
    comments VARCHAR(500),
    evaluation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- A unicidade real é por sessão (UNIQUE acima): uma sessão pertence a um
-- colaborador só, então "uma avaliação por pessoa por sessão" já está contido
-- nela — e um par (colaborador, sessão) permitiria duas notas para a mesma
-- massagem se a sessão trocasse de dono.
CREATE INDEX idx_session_evaluations_company ON session_evaluations (company_id);
CREATE INDEX idx_session_evaluations_collaborator ON session_evaluations (collaborator_id);
CREATE INDEX idx_session_evaluations_date ON session_evaluations (evaluation_date);

COMMIT;
