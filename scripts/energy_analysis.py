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
                
                # Adiciona coluna com nome do arquivo para identificação
                self.data['source_file'] = os.path.basename(self.csv_path_or_pattern)
                
                # Verifica se já existe coluna freq_mhz (novo formato)
                if 'freq_mhz' in self.data.columns:
                    # Novo formato: usa freq_mhz diretamente
                    self.data['cpu_frequency_mhz'] = self.data['freq_mhz']
                    print(f"✓ Formato novo detectado (coluna freq_mhz)")
                else:
                    # Formato antigo: extrai do nome do arquivo
                    frequency = self.extract_frequency_from_filename(os.path.basename(self.csv_path_or_pattern))
                    if frequency is not None:
                        self.data['cpu_frequency_mhz'] = frequency
                        print(f"✓ Formato antigo detectado (CPU: {frequency} MHz do nome do arquivo)")
                    else:
                        self.data['cpu_frequency_mhz'] = 'Não identificado'
                        print(f"✓ Aviso: frequência CPU não encontrada")
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
                    
                    # Verifica se já existe coluna freq_mhz (novo formato)
                    if 'freq_mhz' in df.columns:
                        # Novo formato: usa freq_mhz diretamente
                        df['cpu_frequency_mhz'] = df['freq_mhz']
                        print(f"✓ Carregado: {filename} (formato novo com coluna freq_mhz)")
                    else:
                        # Formato antigo: extrai frequência do nome do arquivo
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
            
            # Exibe informação sobre tamanhos se disponível
            if 'size' in self.data.columns:
                print(f"✓ Tamanhos de entrada detectados: {sorted(self.data['size'].unique())}")
            
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
                
                # Tempo  
                time_mean = np.mean(time_data)
                
                # Energy-Delay Product (EDP) - Métrica de eficiência energética
                # EDP = Energy × Time (lower is better)
                edp_data = energy_data * time_data
                edp_mean = np.mean(edp_data)
                
                # Para uma única amostra, não calculamos desvio padrão ou IC
                if n_samples == 1:
                    results[algo][freq] = {
                        'n_samples': n_samples,
                        'energy': {
                            'mean': energy_mean,
                            'std': None,
                            'ci_lower': None,
                            'ci_upper': None
                        },
                        'time': {
                            'mean': time_mean,
                            'std': None,
                            'ci_lower': None,
                            'ci_upper': None
                        },
                        'edp': {
                            'mean': edp_mean,
                            'std': None,
                            'ci_lower': None,
                            'ci_upper': None
                        },
                        'correlation': {
                            'value': None,
                            'p_value': None
                        }
                    }
                else:
                    # Múltiplas amostras: calcula estatísticas completas
                    energy_std = np.std(energy_data, ddof=1)
                    time_std = np.std(time_data, ddof=1)
                    edp_std = np.std(edp_data, ddof=1)
                    
                    # Correlação
                    correlation, p_value = pearsonr(energy_data, time_data)
                    
                    # Intervalos de confiança bootstrap (95%)
                    rng = np.random.default_rng(42)
                    
                    energy_bootstrap = bootstrap((energy_data,), np.mean, n_resamples=10000,
                                               confidence_level=0.95, random_state=rng)
                    time_bootstrap = bootstrap((time_data,), np.mean, n_resamples=10000,
                                             confidence_level=0.95, random_state=rng)
                    edp_bootstrap = bootstrap((edp_data,), np.mean, n_resamples=10000,
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
                        'edp': {
                            'mean': edp_mean,
                            'std': edp_std,
                            'ci_lower': edp_bootstrap.confidence_interval.low,
                            'ci_upper': edp_bootstrap.confidence_interval.high
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
                print(f"  Valor: {data['energy']['mean']:.6f} J")
                if data['energy']['std'] is not None:
                    print(f"  Desvio: {data['energy']['std']:.6f} J") 
                    print(f"  IC 95%: [{data['energy']['ci_lower']:.6f}, {data['energy']['ci_upper']:.6f}] J")
                
                print(f"\nTempo:")
                print(f"  Valor: {data['time']['mean']:.3f} ms")
                if data['time']['std'] is not None:
                    print(f"  Desvio: {data['time']['std']:.3f} ms")
                    print(f"  IC 95%: [{data['time']['ci_lower']:.3f}, {data['time']['ci_upper']:.3f}] ms")
                
                print(f"\nEDP (Energy-Delay Product):")
                print(f"  Valor: {data['edp']['mean']:.6f} J·ms")
                if data['edp']['std'] is not None:
                    print(f"  Desvio: {data['edp']['std']:.6f} J·ms")
                    print(f"  IC 95%: [{data['edp']['ci_lower']:.6f}, {data['edp']['ci_upper']:.6f}] J·ms")
                print(f"  (Menor EDP = Melhor eficiência energética)")
                
                if data['correlation']['value'] is not None:
                    print(f"\nCorrelação Energia-Tempo: {data['correlation']['value']:.3f} (p={data['correlation']['p_value']:.6f})")
                else:
                    print(f"\nCorrelação Energia-Tempo: N/A (amostra única)")
        
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
                    f.write(f"  Valor: {data['energy']['mean']:.6f} J\n")
                    if data['energy']['std'] is not None:
                        f.write(f"  Desvio: {data['energy']['std']:.6f} J\n")
                        f.write(f"  IC 95%: [{data['energy']['ci_lower']:.6f}, {data['energy']['ci_upper']:.6f}] J\n")
                    f.write("\n")
                    
                    f.write(f"TEMPO:\n")
                    f.write(f"  Valor: {data['time']['mean']:.3f} ms\n")
                    if data['time']['std'] is not None:
                        f.write(f"  Desvio: {data['time']['std']:.3f} ms\n")
                        f.write(f"  IC 95%: [{data['time']['ci_lower']:.3f}, {data['time']['ci_upper']:.3f}] ms\n")
                    f.write("\n")
                    
                    f.write(f"EDP (ENERGY-DELAY PRODUCT):\n")
                    f.write(f"  Valor: {data['edp']['mean']:.6f} J·ms\n")
                    if data['edp']['std'] is not None:
                        f.write(f"  Desvio: {data['edp']['std']:.6f} J·ms\n")
                        f.write(f"  IC 95%: [{data['edp']['ci_lower']:.6f}, {data['edp']['ci_upper']:.6f}] J·ms\n")
                    f.write("  (Menor EDP = Melhor eficiência energética)\n\n")
                    
                    if data['correlation']['value'] is not None:
                        f.write(f"CORRELAÇÃO ENERGIA-TEMPO: {data['correlation']['value']:.3f} ")
                        f.write(f"(p={data['correlation']['p_value']:.6f})\n")
                    else:
                        f.write(f"CORRELAÇÃO ENERGIA-TEMPO: N/A (amostra única)\n")
            
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
                    'energia_valor_J': data['energy']['mean'],
                    'energia_desvio_J': data['energy']['std'] if data['energy']['std'] is not None else '',
                    'energia_ic95_lower_J': data['energy']['ci_lower'] if data['energy']['ci_lower'] is not None else '',
                    'energia_ic95_upper_J': data['energy']['ci_upper'] if data['energy']['ci_upper'] is not None else '',
                    
                    # Tempo
                    'tempo_valor_ms': data['time']['mean'],
                    'tempo_desvio_ms': data['time']['std'] if data['time']['std'] is not None else '',
                    'tempo_ic95_lower_ms': data['time']['ci_lower'] if data['time']['ci_lower'] is not None else '',
                    'tempo_ic95_upper_ms': data['time']['ci_upper'] if data['time']['ci_upper'] is not None else '',
                    
                    # EDP (Energy-Delay Product)
                    'edp_valor_J_ms': data['edp']['mean'],
                    'edp_desvio_J_ms': data['edp']['std'] if data['edp']['std'] is not None else '',
                    'edp_ic95_lower_J_ms': data['edp']['ci_lower'] if data['edp']['ci_lower'] is not None else '',
                    'edp_ic95_upper_J_ms': data['edp']['ci_upper'] if data['edp']['ci_upper'] is not None else '',
                    
                    # Correlação
                    'correlacao_energia_tempo': data['correlation']['value'] if data['correlation']['value'] is not None else '',
                    'correlacao_p_value': data['correlation']['p_value'] if data['correlation']['p_value'] is not None else ''
                }
                
                rows.append(row)
        
        # Cria DataFrame e salva
        df_consolidated = pd.DataFrame(rows)
        df_consolidated.to_csv(output_file, index=False, encoding='utf-8')
        print(f"📊 CSV consolidado salvo em: {output_file}")
        
        return df_consolidated
    
    def analyze_energy_efficiency_edp(self, results):
        """Analisa eficiência energética usando Energy-Delay Product (EDP)."""
        print(f"\n{'='*80}")
        print("ANÁLISE DE EFICIÊNCIA ENERGÉTICA (ENERGY-DELAY PRODUCT)")
        print(f"{'='*80}")
        print("\nEDP = Energia × Tempo (quanto menor, melhor)")
        print("Prioriza energia, mas penaliza algoritmos que demoram muito.\n")
        
        # Coleta todos os EDPs para ranking
        edp_ranking = []
        for algo in results.keys():
            for freq in results[algo].keys():
                edp_ranking.append({
                    'algorithm': algo,
                    'frequency': freq,
                    'edp': results[algo][freq]['edp']['mean'],
                    'energy': results[algo][freq]['energy']['mean'],
                    'time': results[algo][freq]['time']['mean']
                })
        
        # Ordena por EDP (menor = melhor)
        edp_ranking.sort(key=lambda x: x['edp'])
        
        print("="*80)
        print(f"{'RANK':<6} {'ALGORITMO':<20} {'FREQ':<8} {'EDP (J·ms)':<15} {'Energia (J)':<15} {'Tempo (ms)':<12}")
        print("="*80)
        
        for i, entry in enumerate(edp_ranking[:20], 1):  # Top 20
            algo_display = entry['algorithm'].replace('_', ' ').title()
            print(f"{i:<6} {algo_display:<20} {entry['frequency']:<8} "
                  f"{entry['edp']:<15.6f} {entry['energy']:<15.6f} {entry['time']:<12.3f}")
        
        # Análise por algoritmo - encontra melhor frequência para cada
        print(f"\n{'='*80}")
        print("MELHOR FREQUÊNCIA POR ALGORITMO (baseado em EDP)")
        print(f"{'='*80}\n")
        
        for algo in sorted(results.keys()):
            frequencies = sorted(results[algo].keys())
            edps = [(freq, results[algo][freq]['edp']['mean']) for freq in frequencies]
            
            # Encontra frequência com menor EDP
            best_freq, best_edp = min(edps, key=lambda x: x[1])
            worst_freq, worst_edp = max(edps, key=lambda x: x[1])
            
            improvement = ((worst_edp - best_edp) / worst_edp) * 100
            
            algo_name = algo.replace('_', ' ').title()
            print(f"{algo_name}:")
            print(f"  Melhor configuração: {best_freq} MHz (EDP: {best_edp:.6f} J·ms)")
            print(f"  Pior configuração: {worst_freq} MHz (EDP: {worst_edp:.6f} J·ms)")
            print(f"  Melhoria: {improvement:.1f}%")
            print()
        
        return edp_ranking
    
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
    
    def plot_energy_vs_frequency_individual(self, results, output_dir='results/analysis'):
        """Gera gráficos individuais por algoritmo para identificar frequência ótima."""
        
        algorithms = sorted(results.keys())
        colors = ['#2E86AB', '#A23B72', '#F18F01', '#C73E1D', '#6A994E', '#BC4B51']
        
        generated_files = []
        
        for i, algo in enumerate(algorithms):
            # Cria figura individual para cada algoritmo
            fig, ax = plt.subplots(figsize=(10, 6))
            
            frequencies = sorted(results[algo].keys())
            energies = [results[algo][freq]['energy']['mean'] for freq in frequencies]
            
            # Verifica se existem intervalos de confiança
            has_ci = results[algo][frequencies[0]]['energy']['ci_lower'] is not None
            
            color = colors[i % len(colors)]
            
            if has_ci:
                # Intervalos de confiança
                ci_lowers = [results[algo][freq]['energy']['ci_lower'] for freq in frequencies]
                ci_uppers = [results[algo][freq]['energy']['ci_upper'] for freq in frequencies]
                
                # Calcula erros para errorbar (diferenças da média)
                yerr_lower = np.array(energies) - np.array(ci_lowers)
                yerr_upper = np.array(ci_uppers) - np.array(energies)
                yerr = [yerr_lower, yerr_upper]
                
                # Cria gráfico de barras com intervalos de confiança
                bars = ax.bar(range(len(frequencies)), energies, 
                             color=color, alpha=0.8, 
                             yerr=yerr, capsize=8,
                             error_kw={'ecolor': 'black', 'alpha': 0.8})
                
                # Adiciona valores exatos nas barras
                for j, (bar, energy_val) in enumerate(zip(bars, energies)):
                    height = bar.get_height()
                    text_y = height + yerr[1][j] + (max(energies) * 0.05)
                    ax.text(bar.get_x() + bar.get_width()/2., text_y,
                           f'{energy_val:.4f} J', ha='center', va='bottom', 
                           fontsize=10, fontweight='bold')
            else:
                # Sem intervalos de confiança - apenas barras simples
                bars = ax.bar(range(len(frequencies)), energies, 
                             color=color, alpha=0.8)
                
                # Adiciona valores exatos nas barras
                for j, (bar, energy_val) in enumerate(zip(bars, energies)):
                    height = bar.get_height()
                    text_y = height + (max(energies) * 0.05)
                    ax.text(bar.get_x() + bar.get_width()/2., text_y,
                           f'{energy_val:.4f} J', ha='center', va='bottom', 
                           fontsize=10, fontweight='bold')
            
            # Configurações do gráfico
            algo_name = algo.replace('_', ' ').title()
            ax.set_title(f'{algo_name} - Consumo Energético por Frequência CPU', 
                        fontsize=14, fontweight='bold', pad=20)
            ax.set_xlabel('Frequência CPU (MHz)', fontsize=12, fontweight='bold')
            ax.set_ylabel('Consumo de Energia (J)', fontsize=12, fontweight='bold')
            
            # Configurações dos ticks
            ax.set_xticks(range(len(frequencies)))
            ax.set_xticklabels([f'{freq}' for freq in frequencies], fontsize=11)
            ax.grid(True, alpha=0.3, axis='y', linestyle='--')
            
            plt.tight_layout()
            
            # Salva com nome do algoritmo
            output_file = f"{output_dir}/energy_{algo}.png"
            plt.savefig(output_file, dpi=300, bbox_inches='tight')
            plt.close()
            
            generated_files.append(output_file)
        
        return generated_files
    
    def plot_energy_vs_frequency(self, results, output_file='energy_vs_frequency.png'):
        """Gera gráfico consolidado com todos os algoritmos (modo legado)."""
        
        algorithms = sorted(results.keys())
        
        # Limita a 10 algoritmos no gráfico consolidado
        if len(algorithms) > 10:
            print(f"⚠️  Muitos algoritmos ({len(algorithms)}) para gráfico consolidado.")
            print(f"    Gerando apenas gráficos individuais.")
            return
        
        fig, axes = plt.subplots(1, len(algorithms), figsize=(16, 6))
        
        # Se só há um algoritmo, axes não é uma lista
        if len(algorithms) == 1:
            axes = [axes]
        
        colors = ['#2E86AB', '#A23B72', '#F18F01']  # Azul, Roxo, Laranja
        
        for i, algo in enumerate(algorithms):
            ax = axes[i]
            
            frequencies = sorted(results[algo].keys())
            energies = [results[algo][freq]['energy']['mean'] for freq in frequencies]
            
            # Verifica se existem intervalos de confiança
            has_ci = results[algo][frequencies[0]]['energy']['ci_lower'] is not None
            
            if has_ci:
                # Intervalos de confiança
                ci_lowers = [results[algo][freq]['energy']['ci_lower'] for freq in frequencies]
                ci_uppers = [results[algo][freq]['energy']['ci_upper'] for freq in frequencies]
                
                # Calcula erros para errorbar (diferenças da média)
                yerr_lower = np.array(energies) - np.array(ci_lowers)
                yerr_upper = np.array(ci_uppers) - np.array(energies)
                yerr = [yerr_lower, yerr_upper]
                
                # Cria gráfico de barras com intervalos de confiança
                bars = ax.bar(range(len(frequencies)), energies, 
                             color=colors[i % len(colors)], alpha=0.8, 
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
            else:
                # Sem intervalos de confiança - apenas barras simples
                bars = ax.bar(range(len(frequencies)), energies, 
                             color=colors[i % len(colors)], alpha=0.8)
                
                # Adiciona valores exatos nas barras
                for j, (bar, energy_val) in enumerate(zip(bars, energies)):
                    height = bar.get_height()
                    text_y = height + (max(energies) * 0.05)  # 5% de margem extra
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
        
        print(f"📊 Gráfico consolidado salvo em: {output_file}")
    
    def run_analysis(self):
        """Executa apresentação de dados organizados por frequência."""
        print("🔋⏱️ DADOS DE ENERGIA E TEMPO POR FREQUÊNCIA CPU")
        print("=" * 55)
        
        try:
            # Análise principal
            results = self.analyze()
            
            # Análise de eficiência energética (EDP) - NOVA!
            self.analyze_energy_efficiency_edp(results)
            
            # Análise de frequência ótima para processos em segundo plano
            self.analyze_optimal_frequency(results)
            
            # Cria diretório de análise se não existir
            analysis_dir = "results/analysis"
            os.makedirs(analysis_dir, exist_ok=True)
            
            # Gera gráficos individuais por algoritmo (NOVO!)
            print(f"\n📊 Gerando gráficos individuais por algoritmo...")
            individual_files = self.plot_energy_vs_frequency_individual(results, analysis_dir)
            print(f"✓ {len(individual_files)} gráficos individuais gerados")
            
            # Gera gráfico consolidado (se não houver muitos algoritmos)
            self.plot_energy_vs_frequency(results, f"{analysis_dir}/energy_vs_frequency.png")
            
            # Gera relatório de texto
            self.generate_frequency_report(results)
            
            # Gera CSV consolidado
            self.generate_consolidated_csv(results, f"{analysis_dir}/consolidated_data.csv")
            
            print(f"\n" + "="*70)
            print("✅ APRESENTAÇÃO CONCLUÍDA!")
            print(f"\n📄 Relatórios:")
            print(f"   • Texto: energy_frequency_report.txt")
            print(f"   • CSV:   {analysis_dir}/consolidated_data.csv")
            print(f"\n📊 Gráficos:")
            print(f"   • Consolidado: {analysis_dir}/energy_vs_frequency.png")
            print(f"   • Individuais: {analysis_dir}/energy_<algoritmo>.png ({len(individual_files)} arquivos)")
            print("="*70)
            
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

Formatos suportados:
  - Novo formato: CSV com colunas algo,size,freq_mhz,joules,time_ms
  - Formato antigo: CSV com colunas algo,joules,time_ms (frequência extraída do nome do arquivo)
  
O script detecta automaticamente o formato e extrai a frequência CPU apropriadamente.
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
