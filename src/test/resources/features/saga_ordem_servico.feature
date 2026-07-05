# language: pt
Funcionalidade: Saga da Ordem de Serviço

  Cenário: finalizar uma ordem de serviço com pagamento confirmado
    Dado existe uma ordem de serviço recebida
    Quando o diagnóstico é concluído pelo serviço de execução
    E o orçamento é gerado e aprovado pelo serviço financeiro
    E a execução técnica é finalizada
    E o pagamento é solicitado e confirmado
    E o veículo é entregue ao cliente
    Então a saga da ordem de serviço deve finalizar com sucesso
    E os eventos finais de OS e Saga devem ser publicados

  Cenário: compensar uma ordem de serviço com falha operacional antes da finalização
    Dado existe uma ordem de serviço recebida
    Quando o diagnóstico é concluído pelo serviço de execução
    E o orçamento é gerado e aprovado pelo serviço financeiro
    E a execução técnica é iniciada pelo serviço de execução
    E uma falha operacional impede a continuidade antes da finalização
    Então a saga da ordem de serviço deve ser compensada
    E o evento de compensação da Saga deve ser publicado
