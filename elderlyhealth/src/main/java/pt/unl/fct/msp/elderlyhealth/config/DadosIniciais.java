package pt.unl.fct.msp.elderlyhealth.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.unl.fct.msp.elderlyhealth.model.CategoriaDocumento;
import pt.unl.fct.msp.elderlyhealth.model.ContactoEmergencia;
import pt.unl.fct.msp.elderlyhealth.model.ContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.model.MedicaoTensao;
import pt.unl.fct.msp.elderlyhealth.model.Papel;
import pt.unl.fct.msp.elderlyhealth.model.Preferencias;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.repository.ContactoFamiliarRepository;
import pt.unl.fct.msp.elderlyhealth.repository.IdosoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.MedicaoTensaoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.UtilizadorRepository;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.DocumentoService;
import pt.unl.fct.msp.elderlyhealth.service.TensaoArterialService;

/**
 * Dados de demonstração criados ao arrancar:
 * <ul>
 *   <li>admin@elderlyhealth.pt / admin2026 (administração)</li>
 *   <li>enfermeira@elderlyhealth.pt / enfermagem2026 (enfermagem)</li>
 *   <li>joao.silva@mail.pt / familia2026 (filho da Maria Silva)</li>
 * </ul>
 * e dois idosos com uma semana de medições de tensão arterial.
 */
@Component
@ConditionalOnProperty(name = "elderlyhealth.dados-demo", havingValue = "true", matchIfMissing = true)
public class DadosIniciais implements CommandLineRunner {

    private final IdosoRepository idosos;
    private final UtilizadorRepository utilizadores;
    private final MedicaoTensaoRepository medicoes;
    private final ContactoFamiliarRepository contactos;
    private final AuthService auth;
    private final DocumentoService documentos;
    private final Clock clock;

    public DadosIniciais(IdosoRepository idosos, UtilizadorRepository utilizadores, MedicaoTensaoRepository medicoes,
                         ContactoFamiliarRepository contactos, AuthService auth, DocumentoService documentos,
                         Clock clock) {
        this.idosos = idosos;
        this.utilizadores = utilizadores;
        this.medicoes = medicoes;
        this.contactos = contactos;
        this.auth = auth;
        this.documentos = documentos;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (!utilizadores.todos().isEmpty()) {
            return;
        }
        Idoso maria = idoso("Maria Silva", "234567899", LocalDate.of(1942, 5, 17), "Hipertensão; dieta hipossódica",
                "Q204", new Preferencias("Hipossódica", List.of("Penicilina"), "Prefere visitas à tarde"),
                new ContactoEmergencia("João Silva", "912345678", "Filho"));
        Idoso manuel = idoso("Manuel Ferreira", "256789010", LocalDate.of(1938, 11, 2), "Diabetes tipo 2; usa andarilho",
                "Q112", new Preferencias("Sem açúcar", List.of(), "Gosta de jogar às cartas"),
                new ContactoEmergencia("Rosa Ferreira", "936112233", "Filha"));

        auth.criarConta("Administração", "admin@elderlyhealth.pt", "admin2026", Papel.ADMIN);
        auth.criarConta("Enf. Rita Gomes", "enfermeira@elderlyhealth.pt", "enfermagem2026", Papel.ENFERMEIRO);
        Utilizador joao = auth.criarConta("João Silva", "joao.silva@mail.pt", "familia2026", Papel.FAMILIAR);
        joao.getIdosoIds().add(maria.getId());
        utilizadores.guardar(joao);

        ContactoFamiliar c = new ContactoFamiliar();
        c.setIdosoId(maria.getId());
        c.setNome("João Silva");
        c.setTelefone("912345678");
        c.setEmail("joao.silva@mail.pt");
        c.setParentesco("Filho");
        c.setAdicionadoEm(clock.instant());
        contactos.guardar(c);

        historicoTensao(maria, new int[][] {{132, 84}, {128, 82}, {141, 88}, {136, 86}, {129, 80}, {145, 92},
                {138, 85}, {131, 83}, {127, 79}, {134, 84}, {139, 87}, {130, 81}, {126, 80}, {133, 82}});
        historicoTensao(manuel, new int[][] {{118, 76}, {121, 78}, {115, 74}, {119, 77}, {124, 79}, {117, 75},
                {122, 78}, {120, 76}, {116, 74}, {123, 79}, {119, 76}, {121, 77}, {118, 75}, {120, 78}});

        // a pasta fica no disco entre arranques: só cria o exemplo se ainda não existir
        if (documentos.listar(maria.getId(), CategoriaDocumento.HISTORICO_CLINICO).isEmpty()) {
            documentos.guardarTexto(maria.getId(), CategoriaDocumento.HISTORICO_CLINICO, "resumo-clinico.txt",
                "Resumo clínico — Maria Silva\n\nHipertensão arterial diagnosticada em 2009.\n"
                        + "Medicação: losartan 50 mg (manhã).\nAlergia: penicilina.\nDieta hipossódica.\n");
        }
    }

    private Idoso idoso(String nome, String nif, LocalDate nascimento, String info, String quarto,
                        Preferencias pref, ContactoEmergencia emergencia) {
        Idoso i = new Idoso();
        i.setNome(nome);
        i.setNif(nif);
        i.setDataNascimento(nascimento);
        i.setInformacaoMedica(info);
        i.setQuarto(quarto);
        i.setPreferencias(pref);
        i.setContactoEmergencia(emergencia);
        i.setRegistadoEm(clock.instant());
        idosos.guardar(i);
        documentos.criarPastas(i);
        return i;
    }

    /** Duas medições por dia (08:00 e 20:00) durante a última semana. */
    private void historicoTensao(Idoso idoso, int[][] valores) {
        Instant inicio = clock.instant().minus(Duration.ofDays(7));
        for (int k = 0; k < valores.length; k++) {
            MedicaoTensao m = new MedicaoTensao();
            m.setIdosoId(idoso.getId());
            m.setDispositivoId("tensiometro-" + idoso.getQuarto());
            m.setSistolica(valores[k][0]);
            m.setDiastolica(valores[k][1]);
            m.setPulso(68 + (k * 7) % 12);
            m.setClassificacao(TensaoArterialService.classificar(valores[k][0], valores[k][1]));
            m.setRecomendacoes(List.of());
            m.setMedidoEm(inicio.plus(Duration.ofHours(12L * k)));
            medicoes.guardar(m);
        }
    }
}
