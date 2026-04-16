-- Aggiunge il flag is_default a jms_crm_liste.
-- Una sola lista può essere marcata come default (indice parziale univoco).
-- La lista di default raccoglie tutti i contatti non esplicitamente assegnati
-- a una lista specifica durante l'importazione o la creazione manuale.
ALTER TABLE jms_crm_liste
  ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS jms_crm_idx_liste_is_default
  ON jms_crm_liste(is_default)
  WHERE is_default = TRUE;
