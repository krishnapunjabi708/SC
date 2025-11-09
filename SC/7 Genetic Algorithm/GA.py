

import numpy as np
import matplotlib.pyplot as plt

# 1. Fitness Function (maximize)
def fitness(x):
    return x * np.sin(10 * np.pi * x) + 1

# Binary conversion helpers for discrete mutations
def to_binary(x, num_bits=8):
    scaled = int(x * (2**num_bits - 1))
    return format(scaled, f'0{num_bits}b')

def from_binary(bin_str):
    num_bits = len(bin_str)
    scaled = int(bin_str, 2)
    return scaled / (2**num_bits - 1)

# 2. Initialize Population
def initialize_population(pop_size):
    return np.random.rand(pop_size)  # random values between 0 and 1

# 3. Selection Methods
def roulette_selection(pop, fit):
    min_fit = np.min(fit)
    adjusted = fit - min_fit + 0.1  # ensure all positive
    total = np.sum(adjusted)
    r = np.random.random() * total
    current = 0
    for i in range(len(pop)):
        current += adjusted[i]
        if current >= r:
            return pop[i]
    return pop[-1]

def tournament_selection(pop, fit, tournament_size):
    candidates = np.random.choice(len(pop), tournament_size, replace=False)
    best_idx = np.argmax(fit[candidates])
    return pop[candidates[best_idx]]

def rank_selection(pop, fit):
    sorted_indices = np.argsort(fit)
    total_rank = len(pop) * (len(pop) + 1) / 2
    r = np.random.random() * total_rank
    current = 0
    for i in range(len(pop)):
        current += (i + 1)
        if current >= r:
            return pop[sorted_indices[i]]
    return pop[sorted_indices[-1]]

def select(pop, fit, method, tournament_size):
    if method == 'roulette':
        return roulette_selection(pop, fit)
    elif method == 'tournament':
        return tournament_selection(pop, fit, tournament_size)
    elif method == 'rank':
        return rank_selection(pop, fit)
    elif method == 'random':
        return np.random.choice(pop)
    else:
        return tournament_selection(pop, fit, 3)  # default

# 4. Crossover Methods
def crossover(p1, p2, method, crossover_rate=0.8):
    if np.random.random() > crossover_rate:
        return p1  # clone parent
    if method == 'single':
        return p1 if np.random.random() < 0.5 else p2
    elif method == 'arithmetic':
        alpha = np.random.random()
        return np.clip(alpha * p1 + (1 - alpha) * p2, 0, 1)
    elif method == 'uniform':
        return p1 if np.random.random() < 0.5 else p2
    elif method == 'two-point':
        if np.random.random() < 0.33:
            return p1
        elif np.random.random() < 0.5:
            return (p1 + p2) / 2
        else:
            return p2
    else:
        return (p1 + p2) / 2  # default arithmetic

# 5. Mutation Methods
def mutate(x, method, mutation_rate=0.1):
    if np.random.random() >= mutation_rate:
        return x
    if method in ['swap', 'scramble', 'inversion']:
        # Discrete binary mutations
        bin_x = to_binary(x)
        bin_list = list(bin_x)
        if method == 'swap':
            i, j = sorted(np.random.choice(len(bin_list), 2, replace=False))
            bin_list[i], bin_list[j] = bin_list[j], bin_list[i]
        elif method == 'scramble':
            i, j = sorted(np.random.choice(len(bin_list), 2, replace=False))
            substring = bin_list[i:j+1]
            np.random.shuffle(substring)
            bin_list[i:j+1] = substring
        elif method == 'inversion':
            i, j = sorted(np.random.choice(len(bin_list), 2, replace=False))
            substring = bin_list[i:j+1]
            substring.reverse()
            bin_list[i:j+1] = substring
        new_bin = ''.join(bin_list)
        return from_binary(new_bin)
    else:
        # Continuous mutations
        if method == 'gaussian':
            x += np.random.normal(0, 0.1)
        elif method == 'random':
            x = np.random.random()
        elif method == 'bitflip':
            x += (np.random.random() - 0.5) * 0.2
        else:
            x += (np.random.random() - 0.5) * 0.1
    return np.clip(x, 0, 1)

# 6. Steady-state replacement
def steady_state_replace(pop, fit, config, num_replacements):
    new_pop = pop.copy()
    for _ in range(num_replacements):
        p1 = select(pop, fit, config['selection'], config['tournament_size'])
        p2 = select(pop, fit, config['selection'], config['tournament_size'])
        child = crossover(p1, p2, config['crossover'], config['crossover_rate'])
        child = mutate(child, config['mutation'], config['mutation_rate'])
        # Replace worst
        worst_idx = np.argmin(fit)
        new_pop[worst_idx] = child
        fit[worst_idx] = fitness(child)  # Update fit for next iteration
    return new_pop

# 7. Enhanced Genetic Algorithm
def enhanced_genetic_algorithm(config):
    pop_size = config['pop_size']
    generations = config['generations']
    elite_size = config.get('elite_size', 0)
    crossover_rate = config['crossover_rate']
    mutation_rate = config['mutation_rate']
    selection_method = config['selection']
    crossover_method = config['crossover']
    mutation_method = config['mutation']
    tournament_size = config['tournament_size']
    steady_state = config.get('steady_state', False)
    num_replacements = config.get('num_replacements', 1)

    # Initialize
    pop = initialize_population(pop_size)
    best_history = []
    avg_history = []
    for gen in range(generations):
        fit = fitness(pop)
        best_idx = np.argmax(fit)
        best_x = pop[best_idx]
        best_f = fit[best_idx]
        best_history.append(best_f)
        avg_history.append(np.mean(fit))
        print(f"Gen {gen+1:03d} | Best: {best_f:.4f} at x={best_x:.4f}")

        if steady_state:
            pop = steady_state_replace(pop, fit, config, num_replacements)
        else:
            # Generational with elitism
            new_pop = []
            # Elites
            elite_indices = np.argsort(fit)[-elite_size:][::-1]
            for idx in elite_indices:
                new_pop.append(pop[idx])
            # Offspring
            while len(new_pop) < pop_size:
                p1 = select(pop, fit, selection_method, tournament_size)
                p2 = select(pop, fit, selection_method, tournament_size)
                child = crossover(p1, p2, crossover_method, crossover_rate)
                child = mutate(child, mutation_method, mutation_rate)
                new_pop.append(child)
            pop = np.array(new_pop)

    # Final best solution
    final_fit = fitness(pop)
    best_idx = np.argmax(final_fit)
    return pop[best_idx], final_fit[best_idx], best_history, avg_history

if __name__ == "__main__":
    print("Enhanced Genetic Algorithm Optimizer")
    print("Maximizing f(x) = x * sin(10πx) + 1 for x ∈ [0, 1]")

    # User inputs
    config = {}
    config['pop_size'] = int(input("Population Size [30]: ") or 30)
    config['generations'] = int(input("Generations [100]: ") or 100)
    config['elite_size'] = int(input("Elite Size [0]: ") or 0)
    config['crossover_rate'] = float(input("Crossover Rate [0.8]: ") or 0.8)
    config['mutation_rate'] = float(input("Mutation Rate [0.1]: ") or 0.1)

    print("Selection Methods: roulette, tournament, rank, random")
    config['selection'] = input("Selection Method [roulette]: ") or 'roulette'
    if config['selection'] == 'tournament':
        config['tournament_size'] = int(input("Tournament Size [3]: ") or 3)
    else:
        config['tournament_size'] = 3

    print("Crossover Methods: single, arithmetic, uniform, two-point")
    config['crossover'] = input("Crossover Method [arithmetic]: ") or 'arithmetic'

    print("Mutation Types: gaussian, random, bitflip, swap, scramble, inversion")
    config['mutation'] = input("Mutation Type [gaussian]: ") or 'gaussian'

    steady_input = input("Use Steady State? [no]: ").lower().strip()
    config['steady_state'] = 'yes' in steady_input
    if config['steady_state']:
        config['num_replacements'] = int(input("Number of Replacements per Gen [1]: ") or 1)

    print("\nStarting evolution...")
    best_x, best_f, best_history, avg_history = enhanced_genetic_algorithm(config)

    print("\nFinal Result:")
    print(f"Best x = {best_x:.4f}, Best fitness = {best_f:.4f}")

    # Plot fitness evolution
    plt.figure(figsize=(10, 5))
    plt.plot(best_history, label="Best Fitness", color='gold')
    plt.plot(avg_history, label="Average Fitness", color='skyblue')
    plt.title("Fitness Evolution (Enhanced GA)")
    plt.xlabel("Generation")
    plt.ylabel("Fitness")
    plt.legend()
    plt.grid(True)
    plt.show()

    # Plot function and best point
    x = np.linspace(0, 1, 400)
    y = fitness(x)
    plt.figure(figsize=(10, 5))
    plt.plot(x, y, 'b-', label="f(x)")
    plt.scatter(best_x, best_f, color='red', s=100, label="Best Solution", zorder=5)
    plt.legend()
    plt.title("Optimized Function using Enhanced GA")
    plt.xlabel("x")
    plt.ylabel("f(x)")
    plt.grid(True)
    plt.show()

