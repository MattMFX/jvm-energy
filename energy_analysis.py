#!/usr/bin/env python3
"""
Análise Simplificada de Dados de Consumo de Energia
==================================================

Script simplificado que mostra apenas:
- Média
- Desvio Padrão
- Intervalo de Confiança (95%) usando Bootstrap

Autor: Análise de Energia JVM
"""

import pandas as pd
import numpy as np
from scipy.stats import bootstrap
import argparse
import os

class SimpleEnergyAnalysis:
    """Classe para análise simplificada de dados de energia."""
    
    def __init__(self, csv_path: str):
        """
        Inicializa a análise com o arquivo CSV.
        
        Args:
            csv_path: Caminho para o arquivo CSV
        """
        self.csv_path = csv_path
        self.data = None
        self.load_data()
    
    def load_data(self):
        """Carrega os dados do CSV."""
        try:
            self.data = pd.read_csv(self.csv_path)
            print(f"✓ Dados carregados: {len(self.data)} registros")
            print(f"✓ Algoritmos: {', '.join(sorted(self.data['algo'].unique()))}")
        except Exception as e:
            raise ValueError(f"Erro ao carregar dados: {e}")
    
    def analyze(self):
        """Realiza análise simplificada dos dados."""
        print("\n" + "="*60)
        print("ANÁLISE SIMPLIFICADA DE CONSUMO DE ENERGIA")
        print("="*60)
        
        results = {}
        
        for algo in sorted(self.data['algo'].unique()):
            algo_data = self.data[self.data['algo'] == algo]['joules'].values
            
            # Estatísticas básicas
            mean_value = np.mean(algo_data)
            std_value = np.std(algo_data, ddof=1)
            n_samples = len(algo_data)
            
            # Intervalo de confiança bootstrap (95%)
            rng = np.random.default_rng(42)  # Seed para reprodutibilidade
            bootstrap_result = bootstrap((algo_data,), np.mean, n_resamples=10000,
                                       confidence_level=0.95, random_state=rng)
            
            ci_lower = bootstrap_result.confidence_interval.low
            ci_upper = bootstrap_result.confidence_interval.high
            margin_error = (ci_upper - ci_lower) / 2
            
            # Armazena resultados
            results[algo] = {
                'mean': mean_value,
                'std': std_value,
                'ci_lower': ci_lower,
                'ci_upper': ci_upper,
                'margin_error': margin_error,
                'n_samples': n_samples
            }
            
            # Exibe resultados formatados
            print(f"\n{algo.upper().replace('_', ' ')}")
            print("-" * 40)
            print(f"N. observações:     {n_samples:>8}")
            print(f"Média:              {mean_value:>8.6f} J")
            print(f"Desvio Padrão:      {std_value:>8.6f} J")
            print(f"IC 95%:             [{ci_lower:.6f}, {ci_upper:.6f}] J")
            print(f"Margem de erro:     ±{margin_error:.6f} J")
        
        return results
    
    
    def generate_simple_report(self, results, output_file='energy_report.txt'):
        """Gera relatório simplificado."""
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("RELATÓRIO SIMPLIFICADO - CONSUMO DE ENERGIA JVM\n")
            f.write("=" * 55 + "\n\n")
            
            # Informações gerais
            f.write("DADOS ANALISADOS\n")
            f.write("-" * 20 + "\n")
            f.write(f"Arquivo: {self.csv_path}\n")
            f.write(f"Total de registros: {len(self.data)}\n")
            f.write(f"Algoritmos: {', '.join(sorted(self.data['algo'].unique()))}\n\n")
            
            # Resultados por algoritmo
            f.write("RESULTADOS\n")
            f.write("-" * 15 + "\n")
            
            for algo, stats in results.items():
                f.write(f"\n{algo.upper().replace('_', ' ')}:\n")
                f.write(f"  N observações: {stats['n_samples']}\n")
                f.write(f"  Média: {stats['mean']:.6f} J\n")
                f.write(f"  Desvio Padrão: {stats['std']:.6f} J\n")
                f.write(f"  IC 95%: [{stats['ci_lower']:.6f}, {stats['ci_upper']:.6f}] J\n")
                f.write(f"  Margem de erro: ±{stats['margin_error']:.6f} J\n")
            
            # Ranking de eficiência
            f.write(f"\nRANKING DE EFICIÊNCIA (menor consumo = melhor)\n")
            f.write("-" * 50 + "\n")
            
            sorted_algos = sorted(results.items(), key=lambda x: x[1]['mean'])
            for i, (algo, stats) in enumerate(sorted_algos, 1):
                f.write(f"{i}. {algo.replace('_', ' ').title()}: {stats['mean']:.6f} J\n")
            
            # Comparação relativa
            best_algo = sorted_algos[0]
            worst_algo = sorted_algos[-1]
            
            f.write(f"\nCOMPARAÇÃO\n")
            f.write("-" * 15 + "\n")
            f.write(f"Mais eficiente: {best_algo[0].replace('_', ' ').title()}\n")
            f.write(f"Menos eficiente: {worst_algo[0].replace('_', ' ').title()}\n")
            
            efficiency_diff = (worst_algo[1]['mean'] / best_algo[1]['mean'] - 1) * 100
            f.write(f"Diferença: {efficiency_diff:.1f}% mais consumo\n")
    
    def run_analysis(self):
        """Executa análise completa simplificada."""
        print("🔋 ANÁLISE SIMPLIFICADA DE ENERGIA")
        print("=" * 40)
        
        try:
            # Análise principal
            results = self.analyze()
            
            # Relatório
            self.generate_simple_report(results)
            
            print(f"\n" + "="*40)
            print("✅ ANÁLISE CONCLUÍDA!")
            print("="*40)
            
        except Exception as e:
            print(f"❌ Erro: {e}")
            raise


def main():
    """Função principal."""
    parser = argparse.ArgumentParser(description='Análise simplificada de energia')
    parser.add_argument('csv_file', nargs='?', default='target/energy.csv',
                       help='Arquivo CSV (padrão: target/energy.csv)')
    
    args = parser.parse_args()
    
    if not os.path.exists(args.csv_file):
        print(f"❌ Arquivo não encontrado: {args.csv_file}")
        return
    
    try:
        analyzer = SimpleEnergyAnalysis(args.csv_file)
        analyzer.run_analysis()
    except Exception as e:
        print(f"❌ Erro: {e}")


if __name__ == "__main__":
    main()
