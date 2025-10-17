 #!/bin/bash

# Script simples para executar benchmark em um core específico
# Uso: ./run_benchmark_core.sh <core> [opções_benchmark]

# Ajuda
if [ $# -eq 0 ] || [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "Uso: $0 <core> [opções_benchmark]"
    echo ""
    echo "Exemplos:"
    echo "  $0 0                          # Todos os algoritmos no core 0"
    echo "  $0 1 quick_sort_energy        # Só quick sort no core 1"
    echo "  $0 0 -wi 3 -i 5               # Core 0 com menos iterações"
    echo ""
    echo "Cores disponíveis: 0-$(($(nproc)-1))"
    exit 0
fi

# Pegar parâmetros
CORE="$1"
shift
BENCHMARK_ARGS="$@"

# Validação básica
if ! [[ "$CORE" =~ ^[0-9]+$ ]] || [ "$CORE" -ge "$(nproc)" ]; then
    echo "Erro: Core $CORE inválido. Use 0-$(($(nproc)-1))"
    exit 1
fi

# Ir para diretório do projeto
cd "$(dirname "$0")/.."

# Executar benchmark com taskset
echo "Executando no core $CORE..."
taskset -c "$CORE" java -Djava.library.path=. -Djmh.ignoreLock=true -jar target/benchmarks.jar EnergyMeasuredSortingBenchmark $BENCHMARK_ARGS
