# 504091 - Information Technology Project - Semester 1 (2024/2025)
###### _**_Division of Computer Science, Faculty of Information Technology, Ton Duc Thang University_**_

## Project Overview

This project implements novel data mining techniques for extracting high-utility itemsets from transactional datasets. The goal is to mine **Top-K Periodic High-utility Probabilistic Itemsets** from short-time periods while supporting both positive and negative utilities. Our work is motivated by the need to extract actionable insights (e.g., trending item combinations in retail) using advanced utility mining methods.

Our main method is **STP-HUPI** (Short Time Period High Utility Probabilistic Itemsets) – which uses a probability-based expected utility computation combined with period constraints. In addition, we have implemented the additional method for comparison:
- **[*] STP-HUI:** Short Time Period High Utility Itemsets (ignores probability computation).


## Project Information

**Topic:** Short-time Mining of Top-K Periodic High-Utility Probabilistic Itemsets With Both Positive And Negative Utilities

**Guided By:**  
PhD. Nguyen Chi Thien

**Team Members:**
- **Tran Quoc Bao**
    - Student No: 521H0494
    - Email: [521H0494@student.tdtu.edu.vn](mailto:521H0494@student.tdtu.edu.vn) (Group Leader)
- **Le Nguyen Viet Hiep**
    - Student No: 521H0398
    - Email: [538H0398@student.tdtu.edu.vn](mailto:538H0398@student.tdtu.edu.vn)


## Repository

This project is hosted on GitHub. Please refer to the repository for the full source code, documentation, and instructions on running the experiments.

## Project Description

Our project focuses on the mining of top-k high-utility itemsets in a short time period while considering both positive and negative utilities. Traditional high-utility itemset mining algorithms often require a pre-set minimum utility threshold and may not handle negative utilities or time constraints effectively. To address these limitations, our work introduces:

- **STP-HUPI:** Our primary algorithm, which mines itemsets using a combination of probability-based expected utility (which normalizes using the positive utility from transactions) and a period constraint. Only itemsets that occur within a short period (as defined by a maximum period threshold) and have high expected utility are output.
- **STP-HUI:** An alternative algorithm is implemented for comparison with our main method. This method relaxes one of the constraints (probability) to analyze their individual contributions.

## Implementation Details

- **Data Structures:**  
  We use a tree-based structure to represent candidate itemset generation in a depth-first search (DFS) manner.

- **Utility Computation:**
    - The utility of an itemset is computed from raw transaction utilities.
    - For STP-HUPI, a probability is calculated by normalizing the raw utility with the positive utility portion of the transaction.
    - Both positive and negative utilities are processed.

- **Pruning Strategies:**  
  The algorithm dynamically raises the minimum expected utility threshold using threshold raising strategies (e.g., PRIU, PLIU_E, and PLIU_LB) to reduce the search space. A maximum period constraint is also enforced so that only itemsets that occur within a short period are retained.

- **Experimental Evaluation:**  
  Our code includes functions to measure runtime and memory usage for top-k values.

## Requirements

- **Java 8 or later**
- [Lombok](https://projectlombok.org/) for reducing boilerplate code
- [XChart](https://knowm.org/open-source/xchart/) for visualization (if needed)
- Any additional dependencies as specified in the project’s build file

## How to Run

1. **Clone the repository:**

   ```bash
   git clone https://github.com/hiepp1/STP-HUPI.git
   
2. **Build the project:**
  Use your preferred IDE (e.g., Eclipse, IntelliJ IDEA) or build tool (e.g., Maven, Gradle) to compile the code.

3. **Run the main class:**
  The main class (e.g., algorithm.Main) demonstrates how to load datasets, run the STP-HUPI algorithm, and compare it with the alternative methods.

4. **Check the Output:**
  The console will display the top-k itemsets along with their expected utilities, maximum period values, runtime, and memory usage.

## Contact
  If you have any questions or need further clarification, please contact:

- Tran Quoc Bao - [521H0494@student.tdtu.edu.vn](mailto:521H0494@student.tdtu.edu.vn)
- Le Nguyen Viet Hiep - [521H0398@student.tdtu.edu.vn](mailto:521H0398@student.tdtu.edu.vn)

## Acknowledgments
  We would like to thank our guided teacher, PhD. Nguyen Chi Thien, for his support and guidance on this project.