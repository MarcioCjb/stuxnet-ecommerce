package br.com.stuxnet.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import br.com.stuxnet.dto.EnderecoDTO;

public class ViaCepServiceTest {
    private final ViaCepService servico = new ViaCepService();

    @Test
    public void testeBuscarEnderecoValido(){
        EnderecoDTO endereco = servico.consultarCEP("01001-000");
        assertEquals("Praça da Sé", endereco.getLogradouro());
        assertEquals("São Paulo", endereco.getLocalidade());
        assertEquals("SP", endereco.getUf());
    }

    @Test 
    public void testeCepInexistente(){
        assertThrows(IllegalArgumentException.class, () -> servico.consultarCEP("99999999"));
    }
    
    @Test 
    public void testeCepInvalido(){
        assertThrows(IllegalArgumentException.class, () -> servico.consultarCEP("123"));
    }
}
