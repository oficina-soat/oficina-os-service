package br.com.oficina.os.interfaces.presenters;

import br.com.oficina.os.core.entities.usuario.Usuario;
import br.com.oficina.os.interfaces.presenters.view_model.UsuarioViewModel;
import java.util.Comparator;

public class UsuarioPresenterAdapter {
    public UsuarioViewModel usuario(Usuario usuario) {
        return new UsuarioViewModel(
                usuario.id(),
                usuario.pessoa().id(),
                usuario.pessoa().nome(),
                usuario.pessoa().documento().valor(),
                usuario.pessoa().tipoPessoa(),
                usuario.status(),
                usuario.papeis().stream()
                        .map(papel -> papel.valor())
                        .sorted(Comparator.naturalOrder())
                        .toList(),
                usuario.criadoEm(),
                usuario.atualizadoEm());
    }
}
