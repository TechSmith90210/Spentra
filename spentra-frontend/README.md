# Spentra

Spentra is a modern, responsive web application for comprehensive expense tracking and personal budget management. The platform provides users with intuitive tools to log transactions, monitor category-specific budgets, and analyze their financial health over time.

## System Architecture

The frontend is built using Next.js 15 utilizing the App Router architecture. It communicates securely with a RESTful backend service for data persistence and authentication. 

### Technology Stack
- **Framework**: Next.js 15 (App Router, Server/Client Components)
- **Language**: TypeScript
- **Styling**: Tailwind CSS v4 
- **Icons**: Lucide React

## Core Features

- **Financial Dashboard**: Provides an aggregated view of account balances, total income, total expenses, and recent transaction history.
- **Transaction Management**: Supports creation, modification, and deletion of income and expense records. Includes support for recurring transaction types.
- **Budget Tracking**: Allows users to establish monthly spending thresholds per category. Progress bars visually denote actual expenditure against established limits.
- **User Authentication**: Secure JWT-based registration and login system.
- **Configurable Localization**: Dynamic currency support (INR, USD, EUR, GBP) configurable via the user settings panel.
- **Responsive Design**: Fully optimized for cross-device compatibility, featuring mobile-specific navigation patterns (e.g., bottom sheets).

## Local Development Setup

### Prerequisites
- Node.js (v18 or higher)
- npm, yarn, or pnpm

### 1. Installation

Clone the repository and install the required dependencies:

```bash
npm install
```

### 2. Environment Configuration

Create a `.env.local` file in the root directory and specify the backend API URL:

```env
NEXT_PUBLIC_API_BASE_URL=https://spentra-backend.onrender.com
```

### 3. Execution

Start the local development server:

```bash
npm run dev
```

The application will be accessible at [http://localhost:3000](http://localhost:3000).

## Repository Structure

- `/src/app`: Contains Next.js App Router definitions and page layouts.
- `/src/components`: Reusable, stateless UI components (Buttons, Modals, Navigation).
- `/src/features`: Complex, stateful domain-specific components (e.g., Transaction Modals).
- `/src/lib/api`: Centralized HTTP client configurations and API endpoint bindings.
- `/src/providers`: React Context providers for global state (Authentication, Theming, Settings).

---
*Copyright © 2026. All rights reserved.*
