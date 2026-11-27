package br.com.stuxnet.service;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import br.com.stuxnet.dto.FreteDTO;

public class FreteServiceTest {
    private final FreteService servico = new FreteService();
    
    @Test 
    public void testeFreteParaBahia(){
        FreteDTO frete = servico.calcularFrete("48110-000");

        assertEquals("BA", frete.getUf());
        assertEquals(new BigDecimal("22.90"), frete.getValorFrete());
        assertEquals(2, frete.getPrazoDiasUteis());
    }


    @Test 
    public void testeFreteParaSudeste(){
        FreteDTO frete = servico.calcularFrete("01001-000");

        assertEquals("SP", frete.getUf());
        assertEquals(new BigDecimal("34.90"), frete.getValorFrete());
        assertEquals(6, frete.getPrazoDiasUteis());
    }

    @Test 
    public void testeParaNordeste(){
        FreteDTO frete = servico.calcularFrete("51020-001");

        assertEquals("PE", frete.getUf());
        assertEquals(new BigDecimal("22.90"), frete.getValorFrete());
        assertEquals(4, frete.getPrazoDiasUteis());
    }

    @Test 
    public void testarOutros(){
        FreteDTO frete = servico.calcularFrete("70040-010");

        assertEquals("DF", frete.getUf());
        assertEquals(new BigDecimal("49.90"), frete.getValorFrete());
        assertEquals(9, frete.getPrazoDiasUteis());    
    }

    @Test 
    public void testarErroCepInexistente(){
        assertThrows(IllegalArgumentException.class, () -> servico.calcularFrete("99999999"));        
    }
}

