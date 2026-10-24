package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;

/** Ficheiro guardado na pasta de um idoso (os metadados vêm do próprio sistema de ficheiros). */
public record Documento(Long idosoId, CategoriaDocumento categoria, String nome, long tamanhoBytes,
                        String tipo, Instant carregadoEm) {
}
