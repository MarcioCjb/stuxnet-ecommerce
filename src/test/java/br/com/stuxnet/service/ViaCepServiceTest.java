package br.com.stuxnet.service;

import br.com.stuxnet.dto.EnderecoDTO;
import br.com.stuxnet.exception.ApiException;
import br.com.stuxnet.support.ViaCepStub;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ViaCepServiceTest {
    private ViaCepStub stub;
    private ViaCepService servico;
    @BeforeEach void iniciar() throws Exception { stub = new ViaCepStub(); servico = stub.service(); }
    @AfterEach void parar() { stub.close(); }

    @Test void buscaEnderecoComAcentosENormalizaCep() {
        EnderecoDTO endereco = servico.consultarCEP("01001-000");
        assertEquals("Praça da Sé", endereco.getLogradouro());
        assertEquals("São Paulo", endereco.getLocalidade());
        assertEquals("SP", endereco.getUf());
    }
    @Test void cepInexistenteTemStatus404ComErroBooleanoOuTexto() {
        for (String cep : new String[]{"99999999", "55555555"}) {
            assertEquals(404, assertThrows(ApiException.class, () -> servico.consultarCEP(cep)).getStatus());
        }
    }
    @Test void cepMalformadoNaoFazRequisicaoExterna() {
        for (String cep : new String[]{null, "", "123", "abc01001000", "01001000abc", "01001 000"}) {
            assertThrows(IllegalArgumentException.class, () -> servico.consultarCEP(cep));
        }
        assertEquals(0, stub.chamadas.get());
    }
    @Test void falhasDoProvedorSao502() {
        for (String cep : new String[]{"11111111", "22222222", "33333333", "44444444"}) {
            assertEquals(502, assertThrows(ApiException.class, () -> servico.consultarCEP(cep)).getStatus());
        }
    }
}
