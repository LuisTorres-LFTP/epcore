package com.epcore.multicliente;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Lista de bases a las que EpCore debe aplicar tareas de arranque (principal + clientes). */
@Component
public class ClientesConfigurados {

    private final MultiClienteProperties props;
    private final TransactionTemplate tx;

    public ClientesConfigurados(MultiClienteProperties props, TransactionTemplate tx) {
        this.props = props;
        this.tx = tx;
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>();
        ids.add(ClienteContexto.PRINCIPAL);
        if (props.isHabilitado()) {
            ids.addAll(props.getClientes().keySet());
        }
        return ids;
    }

    /** Ejecuta la tarea en una transacción propia dentro de la base de cada cliente. */
    public void paraCadaCliente(Consumer<String> tarea) {
        for (String id : ids()) {
            ClienteContexto.ejecutarComo(id, () -> tx.executeWithoutResult(s -> tarea.accept(id)));
        }
    }

    public MultiClienteProperties propiedades() {
        return props;
    }
}
