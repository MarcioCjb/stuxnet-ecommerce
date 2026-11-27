package br.com.stuxnet.command;

import br.com.stuxnet.dto.SaudeDTO;
import br.com.stuxnet.infra.http.Requisicao;
import br.com.stuxnet.infra.http.RespostaHttp;

public final class SaudeCommand implements Command {
    @Override
    public RespostaHttp executar(Requisicao requisicao) {
        return RespostaHttp.ok(new SaudeDTO("StuxNet", "UP"));
    }
}
