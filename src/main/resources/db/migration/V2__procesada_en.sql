-- ============================================================
--  V2: notificada_en pasa a llamarse procesada_en
--
--  Cambia el significado, no solo el nombre. Antes la columna marcaba
--  "te la he enviado". Ahora marca "la he evaluado en el resumen de ese
--  dia", entrase o no.
--
--  El motivo: el resumen tiene ahora dos secciones, y una de ellas acepta
--  ofertas con encaje bajo. Si solo se marcasen las enviadas, el resto se
--  reevaluaria cada manana para siempre. Y el veredicto es determinista:
--  lo que no entro hoy no va a entrar manana.
-- ============================================================

ALTER TABLE oferta RENAME COLUMN notificada_en TO procesada_en;

DROP INDEX IF EXISTS ix_oferta_pendientes;

CREATE INDEX ix_oferta_pendientes
    ON oferta (encaje DESC)
    WHERE procesada_en IS NULL;
