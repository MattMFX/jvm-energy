#!/usr/bin/env python3
"""
Apresentação de Dados de Consumo de Energia e Tempo por Frequência
=================================================================

Script que apresenta métricas organizadas por:
- Algoritmo (bubble_sort, merge_sort, quick_sort)  
- Frequência do processador (extraída do nome do arquivo)
- Métricas: Energia, Tempo, Correlação Energia-Tempo

Autor: Análise de Energia JVM
"""

import pandas as pd
import numpy as np
from scipy.stats import bootstrap, pearsonr
import argparse
import os
import glob
import re
from pathlib import Path
import matplotlib.pyplot as plt

class EnergyTimeAnalysis:
    """Classe para apresentação de dados de energia e tempo por frequência."""
    
    def __init__(self, csv_path_or_pattern: str):
        """
        Inicializa a análise com arquivo(s) CSV.
        
        Args:
            csv_path_or_pattern: Caminho para arquivo CSV ou padrão para múltiplos arquivos
        """
        self.csv_path_or_pattern = csv_path_or_pattern
        self.data = None
        self.load_data()
    
    def extract_frequency_from_filename(self, filename):
        """Extrai a frequência do processador do nome do arquivo."""
        # Busca por padrão como "500mhz", "1500mhz", etc.
        match = re.search(r'(\d+)mhz', filename.lower())
        if match:
            return int(match.group(1))
        return None
    
    def load_data(self):
        """Carrega os dados de um ou múltiplos CSVs."""
        try:
            # Verifica se é um arquivo único
            if os.path.isfile(self.csv_path_or_pattern):
                # Arquivo único
                self.data = pd.read_csv(self.csv_path_or_pattern)
                print(f"✓ Arquivo carregado: {self.csv_path_or_pattern}")
            else:
                # Múltiplos arquivos - primeiro tenta como diretório
                if os.path.isdir(self.csv_path_or_pattern):
                    files = glob.glob(os.path.join(self.csv_path_or_pattern, "*.csv"))
                else:
                    # Tenta como padrão glob
                    files = glob.glob(self.csv_path_or_pattern)
                
                if not files:
                    raise FileNotFoundError(f"Nenhum arquivo CSV encontrado para: {self.csv_path_or_pattern}")
                
                # Carrega e combina todos os arquivos
                dataframes = []
                for file in sorted(files):
                    df = pd.read_csv(file)
                    filename = os.path.basename(file)
                    
                    # Adiciona coluna com nome do arquivo para identificação
                    df['source_file'] = filename
                    
                    # Extrai e adiciona frequência do processador
                    frequency = self.extract_frequency_from_filename(filename)
                    if frequency is not None:
                        df['cpu_frequency_mhz'] = frequency
                        print(f"✓ Carregado: {filename} (CPU: {frequency} MHz)")
                    else:
                        df['cpu_frequency_mhz'] = 'Não identificado'
                        print(f"✓ Carregado: {filename} (CPU: frequência não identificada)")
                    
                    dataframes.append(df)
                
                if not dataframes:
                    raise ValueError("Nenhum arquivo CSV válido encontrado")
                
                self.data = pd.concat(dataframes, ignore_index=True)
            
            print(f"✓ Total de registros: {len(self.data)}")
            print(f"✓ Algoritmos: {', '.join(sorted(self.data['algo'].unique()))}")
            frequencies = sorted([f for f in self.data['cpu_frequency_mhz'].unique() if f != 'Não identificado'])
            if frequencies:
                print(f"✓ Frequências CPU: {', '.join(map(str, frequencies))} MHz")
            
            # Verifica se tem as colunas necessárias
            required_columns = ['algo', 'joules', 'time_ms']
            missing_columns = [col for col in required_columns if col not in self.data.columns]
            if missing_columns:
                raise ValueError(f"Colunas obrigatórias não encontradas: {missing_columns}")
                
        except Exception as e:
            raise ValueError(f"Erro ao carregar dados: {e}")
    
    def analyze(self):
        """Apresenta métricas organizadas por algoritmo e frequência CPU."""
        print("\n" + "="*80)
        print("DADOS DE CONSUMO DE ENERGIA E TEMPO POR FREQUÊNCIA CPU")
        print("="*80)
        
        results = {}
        
        # Agrupa por algoritmo e frequência
        for algo in sorted(self.data['algo'].unique()):
            results[algo] = {}
            
            frequencies = sorted([f for f in self.data['cpu_frequency_mhz'].unique() if f != 'Não identificado'])
            
            for freq in frequencies:
                subset = self.data[(self.data['algo'] == algo) & (self.data['cpu_frequency_mhz'] == freq)]
                
                if len(subset) == 0:
                    continue
                    
                energy_data = subset['joules'].values
                time_data = subset['time_ms'].values
                
                # Estatísticas básicas
                n_samples = len(energy_data)
                
                # Energia
                energy_mean = np.mean(energy_data)
                energy_std = np.std(energy_data, ddof=1)
                
                # Tempo  
                time_mean = np.mean(time_data)
                time_std = np.std(time_data, ddof=1)
                
                # Correlação
                correlation, p_value = pearsonr(energy_data, time_data)
                
                # Intervalos de confiança bootstrap (95%)
                rng = np.random.default_rng(42)
                
                energy_bootstrap = bootstrap((energy_data,), np.mean, n_resamples=10000,
                                           confidence_level=0.95, random_state=rng)
                time_bootstrap = bootstrap((time_data,), np.mean, n_resamples=10000,
                                         confidence_level=0.95, random_state=rng)
                
                # Armazena resultados
                results[algo][freq] = {
                    'n_samples': n_samples,
                    'energy': {
                        'mean': energy_mean,
                        'std': energy_std,
                        'ci_lower': energy_bootstrap.confidence_interval.low,
                        'ci_upper': energy_bootstrap.confidence_interval.high
                    },
                    'time': {
                        'mean': time_mean,
                        'std': time_std,
                        'ci_lower': time_bootstrap.confidence_interval.low,
                        'ci_upper': time_bootstrap.confidence_interval.high
                    },
                    'correlation': {
                        'value': correlation,
                        'p_value': p_value
                    }
                }
        
        # Apresenta dados organizados
        for algo in sorted(results.keys()):
            print(f"\n{algo.upper().replace('_', ' ')}")
            print("=" * 60)
            
            for freq in sorted(results[algo].keys()):
                data = results[algo][freq]
                
                print(f"\nCPU: {freq} MHz")
                print("-" * 40)
                print(f"Observações: {data['n_samples']}")
                
                print(f"\nEnergia:")
                print(f"  Média: {data['energy']['mean']:.6f} J")
                print(f"  Desvio: {data['energy']['std']:.6f} J") 
                print(f"  IC 95%: [{data['energy']['ci_lower']:.6f}, {data['energy']['ci_upper']:.6f}] J")
                
                print(f"\nTempo:")
                print(f"  Média: {data['time']['mean']:.3f} ms")
                print(f"  Desvio: {data['time']['std']:.3f} ms")
                print(f"  IC 95%: [{data['time']['ci_lower']:.3f}, {data['time']['ci_upper']:.3f}] ms")
                
                print(f"\nCorrelação Energia-Tempo: {data['correlation']['value']:.3f} (p={data['correlation']['p_value']:.6f})")
        
        return results
    
    
    def generate_frequency_report(self, results, output_file='energy_frequency_report.txt'):
        """Gera relatório organizado por frequência CPU."""
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("RELATÓRIO - DADOS POR FREQUÊNCIA CPU\n")
            f.write("=" * 45 + "\n\n")
            
            # Informações gerais
            f.write("DADOS ANALISADOS\n")
            f.write("-" * 20 + "\n")
            f.write(f"Fonte: {self.csv_path_or_pattern}\n")
            f.write(f"Total de registros: {len(self.data)}\n")
            f.write(f"Algoritmos: {', '.join(sorted(self.data['algo'].unique()))}\n")
            frequencies = sorted([f for f in self.data['cpu_frequency_mhz'].unique() if f != 'Não identificado'])
            if frequencies:
                f.write(f"Frequências CPU: {', '.join(map(str, frequencies))} MHz\n\n")
            
            # Dados por algoritmo e frequência
            for algo in sorted(results.keys()):
                f.write(f"\n{algo.upper().replace('_', ' ')}\n")
                f.write("=" * 50 + "\n")
                
                for freq in sorted(results[algo].keys()):
                    data = results[algo][freq]
                    
                    f.write(f"\nCPU: {freq} MHz\n")
                    f.write("-" * 30 + "\n")
                    f.write(f"Observações: {data['n_samples']}\n\n")
                    
                    f.write(f"ENERGIA:\n")
                    f.write(f"  Média: {data['energy']['mean']:.6f} J\n")
                    f.write(f"  Desvio: {data['energy']['std']:.6f} J\n")
                    f.write(f"  IC 95%: [{data['energy']['ci_lower']:.6f}, {data['energy']['ci_upper']:.6f}] J\n\n")
                    
                    f.write(f"TEMPO:\n")
                    f.write(f"  Média: {data['time']['mean']:.3f} ms\n")
                    f.write(f"  Desvio: {data['time']['std']:.3f} ms\n")
                    f.write(f"  IC 95%: [{data['time']['ci_lower']:.3f}, {data['time']['ci_upper']:.3f}] ms\n\n")
                    
                    f.write(f"CORRELAÇÃO ENERGIA-TEMPO: {data['correlation']['value']:.3f} ")
                    f.write(f"(p={data['correlation']['p_value']:.6f})\n")
            
            # Tabela resumo por frequência
            f.write(f"\n\nTABELA RESUMO POR FREQUÊNCIA\n")
            f.write("=" * 40 + "\n")
            
            # Cabeçalho
            f.write(f"\n{'Algoritmo':<15} {'CPU(MHz)':<10} {'Energia(J)':<15} {'Tempo(ms)':<12}\n")
            f.write("-" * 55 + "\n")
            
            for algo in sorted(results.keys()):
                for freq in sorted(results[algo].keys()):
                    data = results[algo][freq]
                    f.write(f"{algo:<15} {freq:<10} {data['energy']['mean']:<15.6f} ")
                    f.write(f"{data['time']['mean']:<12.3f}\n")
                f.write("\n")
    
    def generate_consolidated_csv(self, results, output_file='consolidated_data.csv'):
        """Gera CSV consolidado com todas as métricas."""
        rows = []
        
        for algo in sorted(results.keys()):
            for freq in sorted(results[algo].keys()):
                data = results[algo][freq]
                
                row = {
                    'algoritmo': algo,
                    'cpu_frequency_mhz': freq,
                    'n_observacoes': data['n_samples'],
                    
                    # Energia
                    'energia_media_J': data['energy']['mean'],
                    'energia_desvio_J': data['energy']['std'],
                    'energia_ic95_lower_J': data['energy']['ci_lower'],
                    'energia_ic95_upper_J': data['energy']['ci_upper'],
                    
                    # Tempo
                    'tempo_medio_ms': data['time']['mean'],
                    'tempo_desvio_ms': data['time']['std'],
                    'tempo_ic95_lower_ms': data['time']['ci_lower'],
                    'tempo_ic95_upper_ms': data['time']['ci_upper'],
                    
                    # Correlação
                    'correlacao_energia_tempo': data['correlation']['value'],
                    'correlacao_p_value': data['correlation']['p_value']
                }
                
                rows.append(row)
        
        # Cria DataFrame e salva
        df_consolidated = pd.DataFrame(rows)
        df_consolidated.to_csv(output_file, index=False, encoding='utf-8')
        print(f"📊 CSV consolidado salvo em: {output_file}")
        
        return df_consolidated
    
    def analyze_optimal_frequency(self, results):
        """Analisa frequência ótima para processos em segundo plano."""
        print(f"\n{'='*60}")
        print("ANÁLISE DE FREQUÊNCIA ÓTIMA PARA PROCESSOS EM SEGUNDO PLANO")
        print(f"{'='*60}")
        print("\n(Foco: minimizar consumo energético, tempo não é crítico)\n")
        
        optimal_frequencies = {}
        
        for algo in sorted(results.keys()):
            frequencies = sorted(results[algo].keys())
            energies = [results[algo][freq]['energy']['mean'] for freq in frequencies]
            
            # Encontra frequência com menor consumo energético
            min_energy_idx = energies.index(min(energies))
            optimal_freq = frequencies[min_energy_idx]
            min_energy = energies[min_energy_idx]
            
            # Calcula economia energética comparado à frequência mais alta
            max_freq = frequencies[-1]
            max_freq_energy = results[algo][max_freq]['energy']['mean']
            energy_savings = ((max_freq_energy - min_energy) / max_freq_energy) * 100
            
            optimal_frequencies[algo] = {
                'optimal_freq': optimal_freq,
                'min_energy': min_energy,
                'max_freq': max_freq,
                'max_freq_energy': max_freq_energy,
                'energy_savings': energy_savings
            }
            
            # Exibe resultado
            algo_name = algo.replace('_', ' ').title()
            print(f"{algo_name}:")
            print(f"  Frequência ótima: {optimal_freq} MHz")
            print(f"  Consumo mínimo: {min_energy:.6f} J")
            print(f"  Economia vs {max_freq} MHz: {energy_savings:.1f}%")
            print()
        
        return optimal_frequencies
    
    def plot_energy_vs_frequency(self, results, output_file='energy_vs_frequency.png'):
        """Gera gráficos de barras por algoritmo para identificar frequência ótima."""
        
        algorithms = sorted(results.keys())
        fig, axes = plt.subplots(1, len(algorithms), figsize=(16, 6))
        
        # Se só há um algoritmo, axes não é uma lista
        if len(algorithms) == 1:
            axes = [axes]
        
        colors = ['#2E86AB', '#A23B72', '#F18F01']  # Azul, Roxo, Laranja
        
        for i, algo in enumerate(algorithms):
            ax = axes[i]
            
            frequencies = sorted(results[algo].keys())
            energies = [results[algo][freq]['energy']['mean'] for freq in frequencies]
            
            # Intervalos de confiança
            ci_lowers = [results[algo][freq]['energy']['ci_lower'] for freq in frequencies]
            ci_uppers = [results[algo][freq]['energy']['ci_upper'] for freq in frequencies]
            
            # Calcula erros para errorbar (diferenças da média)
            yerr_lower = np.array(energies) - np.array(ci_lowers)
            yerr_upper = np.array(ci_uppers) - np.array(energies)
            yerr = [yerr_lower, yerr_upper]
            
            # Cria gráfico de barras com intervalos de confiança
            bars = ax.bar(range(len(frequencies)), energies, 
                         color=colors[i], alpha=0.8, 
                         yerr=yerr, capsize=8,
                         error_kw={'ecolor': 'black', 'alpha': 0.8})
            
            # Adiciona valores exatos nas barras
            for j, (bar, energy_val) in enumerate(zip(bars, energies)):
                height = bar.get_height()
                # Posiciona o texto acima da barra (considerando a margem das barras de erro)
                text_y = height + yerr[1][j] + (max(energies) * 0.05)  # 5% de margem extra
                ax.text(bar.get_x() + bar.get_width()/2., text_y,
                       f'{energy_val:.4f}', ha='center', va='bottom', 
                       fontsize=9, fontweight='bold', rotation=0)
            
            # Configurações do subplot
            algo_name = algo.replace('_', ' ').title()
            ax.set_title(f'{algo_name}', fontsize=14, fontweight='bold')
            ax.set_xlabel('Frequência CPU (MHz)', fontsize=12, fontweight='bold')
            
            # Apenas o primeiro gráfico tem label do eixo Y
            if i == 0:
                ax.set_ylabel('Consumo de Energia (J)', fontsize=12, fontweight='bold')
            
            # Configurações dos ticks
            ax.set_xticks(range(len(frequencies)))
            ax.set_xticklabels([f'{freq}' for freq in frequencies])
            ax.grid(True, alpha=0.3, axis='y')
        
        # Título geral
        fig.suptitle('Consumo Energético por Frequência CPU', 
                     fontsize=16, fontweight='bold', y=1.02)
        
        plt.tight_layout()
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        plt.close()
        
        print(f"📊 Gráfico salvo em: {output_file}")
    
    def run_analysis(self):
        """Executa apresentação de dados organizados por frequência."""
        print("🔋⏱️ DADOS DE ENERGIA E TEMPO POR FREQUÊNCIA CPU")
        print("=" * 55)
        
        try:
            # Análise principal
            results = self.analyze()
            
            # Análise de frequência ótima para processos em segundo plano
            self.analyze_optimal_frequency(results)
            
            # Cria diretório de análise se não existir
            analysis_dir = "results/analysis"
            os.makedirs(analysis_dir, exist_ok=True)
            
            # Gera gráfico de energia vs frequência
            self.plot_energy_vs_frequency(results, f"{analysis_dir}/energy_vs_frequency.png")
            
            # Gera relatório de texto
            self.generate_frequency_report(results)
            
            # Gera CSV consolidado
            self.generate_consolidated_csv(results, f"{analysis_dir}/consolidated_data.csv")
            
            print(f"\n" + "="*55)
            print("✅ APRESENTAÇÃO CONCLUÍDA!")
            print(f"📄 Relatório: energy_frequency_report.txt")
            print(f"📊 CSV consolidado: {analysis_dir}/consolidated_data.csv")
            print(f"📈 Gráfico de frequência ótima: {analysis_dir}/energy_vs_frequency.png")
            print("="*55)
            
        except Exception as e:
            print(f"❌ Erro: {e}")
            raise


def main():
    """Função principal."""
    parser = argparse.ArgumentParser(
        description='Apresentação de dados de energia e tempo organizados por frequência CPU',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Exemplos de uso:
  python energy_analysis.py arquivo.csv                  # Um arquivo específico
  python energy_analysis.py "results/*.csv"              # Todos CSVs na pasta results
  python energy_analysis.py results/                     # Todos CSVs na pasta results
  python energy_analysis.py                              # Usa padrão: results/
  
O script extrai automaticamente a frequência CPU do nome do arquivo (ex: *-500mhz.csv)
        """
    )
    parser.add_argument('csv_source', nargs='?', default='results/',
                       help='Arquivo CSV, padrão glob, ou diretório (padrão: results/)')
    
    args = parser.parse_args()
    
    # Verifica se existe o arquivo/diretório/padrão
    if not (os.path.exists(args.csv_source) or glob.glob(args.csv_source)):
        print(f"❌ Fonte não encontrada: {args.csv_source}")
        print("💡 Dica: Use 'results/' para analisar todos os CSVs da pasta results")
        return
    
    try:
        analyzer = EnergyTimeAnalysis(args.csv_source)
        analyzer.run_analysis()
    except Exception as e:
        print(f"❌ Erro: {e}")


if __name__ == "__main__":
    main()
