-- ============================================================
--  V1: esquema inicial de RedTrack
--
--  Una fila por oferta CANONICA en `oferta`, y una fila por cada
--  aparicion de esa oferta en `oferta_fuente`. Que la misma oferta
--  este en varios portales es informacion util, no basura.
-- ============================================================

CREATE TABLE oferta (
    id                    BIGSERIAL PRIMARY KEY,

    fuente                VARCHAR(40)  NOT NULL,
    id_externo            VARCHAR(200) NOT NULL,

    titulo                VARCHAR(500) NOT NULL,
    empresa               VARCHAR(300),
    ubicacion             VARCHAR(300),
    modalidad             VARCHAR(20)  NOT NULL DEFAULT 'DESCONOCIDA',

    -- Siempre bruto anual en euros, venga como venga de la fuente.
    salario_min           INTEGER,
    salario_max           INTEGER,

    descripcion           TEXT,
    url                   VARCHAR(1000),
    idioma                VARCHAR(5),

    publicada_en          TIMESTAMPTZ,
    capturada_en          TIMESTAMPTZ  NOT NULL,

    -- sha256(empresa|titulo|ubicacion) normalizados. Paso 1 de la deduplicacion.
    -- VARCHAR y no CHAR: CHAR rellena con espacios hasta la longitud fija, y
    -- eso no se quiere cerca de un hash.
    huella                VARCHAR(64)  NOT NULL,

    -- Claves de cotejo del paso 2: acotan las candidatas antes de aplicar
    -- Jaro-Winkler, para no traerse la tabla entera a memoria.
    empresa_normalizada   VARCHAR(300),
    ubicacion_normalizada VARCHAR(300),

    -- --- Analisis ---
    encaje                INTEGER      NOT NULL DEFAULT 0,
    senal                 VARCHAR(10),
    anos_requeridos       INTEGER,
    tecnologias_pedidas   TEXT,
    las_tengo             TEXT,
    me_faltan             TEXT,
    banderas_rojas        TEXT,
    resumen_ia            TEXT,

    -- NULL mientras no se haya enviado. Es lo que evita repetir ofertas.
    notificada_en         TIMESTAMPTZ,

    CONSTRAINT uk_oferta_huella UNIQUE (huella),
    CONSTRAINT uk_oferta_fuente_id_externo UNIQUE (fuente, id_externo),
    CONSTRAINT ck_oferta_encaje CHECK (encaje BETWEEN 0 AND 100)
);

-- Consulta del resumen diario: pendientes por encima del umbral.
CREATE INDEX ix_oferta_pendientes
    ON oferta (encaje DESC)
    WHERE notificada_en IS NULL;

-- Paso 2 de la deduplicacion.
CREATE INDEX ix_oferta_cotejo
    ON oferta (empresa_normalizada, ubicacion_normalizada);

CREATE INDEX ix_oferta_publicada_en ON oferta (publicada_en DESC);


CREATE TABLE oferta_fuente (
    id         BIGSERIAL PRIMARY KEY,
    oferta_id  BIGINT       NOT NULL,
    fuente     VARCHAR(40)  NOT NULL,
    id_externo VARCHAR(200) NOT NULL,
    url        VARCHAR(1000),
    vista_en   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_oferta_fuente_oferta
        FOREIGN KEY (oferta_id) REFERENCES oferta (id) ON DELETE CASCADE,
    CONSTRAINT uk_oferta_fuente UNIQUE (fuente, id_externo)
);

CREATE INDEX ix_oferta_fuente_oferta ON oferta_fuente (oferta_id);
