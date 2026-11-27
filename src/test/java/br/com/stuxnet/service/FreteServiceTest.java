package br.com.stuxnet.service;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import br.com.stuxnet.dto.FreteDTO;
import br.com.stuxnet.support.ViaCepStub;

public class FreteServiceTest {
    private ViaCepStub stub;
    private FreteService servico;

    @BeforeEach
    void iniciar() throws Exception {
        stub = new ViaCepStub();
        servico = new FreteService(stub.service());
    }

    @AfterEach
    void parar() { stub.close(); }

    @Test
    public void testeFreteParaBahia(){
        FreteDTO frete = servico.consultar("48110-000", "NORMAL");

        assertEquals("BA", frete.uf());
        assertEquals(new BigDecimal("22.90"), frete.valor());
        assertEquals(2, frete.prazoDiasUteis());
    }


    @Test
    public void testeFreteParaSudeste(){
        FreteDTO frete = servico.consultar("01001-000", "NORMAL");

        assertEquals("SP", frete.uf());
        assertEquals(new BigDecimal("34.90"), frete.valor());
        assertEquals(6, frete.prazoDiasUteis());
    }

    @Test
    public void testeParaNordeste(){
        FreteDTO frete = servico.consultar("51020-001", "NORMAL");

        assertEquals("PE", frete.uf());
        assertEquals(new BigDecimal("22.90"), frete.valor());
        assertEquals(4, frete.prazoDiasUteis());
    }

    @Test
    public void testarOutros(){
        FreteDTO frete = servico.consultar("70040-010", "NORMAL");

        assertEquals("DF", frete.uf());
        assertEquals(new BigDecimal("49.90"), frete.valor());
        assertEquals(9, frete.prazoDiasUteis());
    }

    @Test
    public void testarErroCepInexistente(){
        assertThrows(IllegalArgumentException.class, () -> servico.consultar("99999999", "NORMAL"));
    }
}
