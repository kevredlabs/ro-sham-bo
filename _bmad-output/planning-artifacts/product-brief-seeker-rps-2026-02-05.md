---
stepsCompleted: [1, 2]
inputDocuments: []
date: 2026-02-05
author: Léo
---

# Product Brief: seeker-rps

## Executive Summary

**seeker-rps** is a casual rock-paper-scissors mobile game designed specifically for the Solana Seeker device community. The product addresses the need for simple, fun, and social gaming experiences during Solana events and gatherings. By leveraging the Seeker's integrated wallet and focusing on minimal transaction overhead (only 2 transactions per game), the app delivers an instant, Web2-like gaming experience while maintaining the security benefits of blockchain escrow for bet amounts.

The core innovation lies in the hybrid architecture: onchain escrow for fund security combined with offchain gameplay for instant responsiveness. Players can create or join games by exchanging simple 6-character codes (via manual entry, QR code, or potentially NFC), place bets in SOL, play instantly without blockchain delays, and automatically receive winnings through the smart contract. The target audience is Seeker device owners who value simplicity, social interaction, and fun over complex DeFi mechanics.

With a tech stack optimized for rapid development (Kotlin for mobile, Rust for backend, Rust + Anchor for smart contracts), the MVP focuses on delivering the core 1v1 gameplay loop with maximum UX simplicity, setting the foundation for future features like tournaments, leaderboards, and community discovery.

---

## Core Vision

### Problem Statement

Solana community members, particularly Seeker device owners, lack simple and engaging ways to have fun together during events, meetups, and social gatherings. While the blockchain ecosystem offers complex DeFi protocols and gaming platforms, there's a gap for casual, instant, social experiences that feel as smooth as Web2 games but leverage crypto's unique benefits (trustless escrow, transparent outcomes).

Current solutions for friendly betting or gaming between peers require manual address exchanges, multiple transaction confirmations that kill momentum, complex wallet connections, or reliance on centralized platforms that don't integrate with the Seeker ecosystem. The result is that despite having crypto in their pockets, users can't quickly and easily engage in simple, fun, low-stakes games with people right in front of them.

### Problem Impact

**For Seeker Owners:**
- The device's integrated wallet and unique hardware capabilities are underutilized for social, casual use cases
- Missing opportunities to showcase Solana's speed and low fees in fun, accessible scenarios
- No compelling "party trick" app to demonstrate blockchain utility to newcomers

**For Solana Events:**
- Gatherings remain focused on serious discussions without light, social gaming moments
- Lack of icebreaker activities that naturally incorporate crypto
- Missed opportunity to demonstrate blockchain UX improvements to skeptics

**For the Broader Ecosystem:**
- Casual users are deterred by the learning curve of most blockchain apps
- Gaming on Solana is associated with complex mechanics rather than accessibility
- Gap between "Web3 should be fun" messaging and actual user-friendly products

### Why Existing Solutions Fall Short

**Web2 Gaming Apps:**
- Don't integrate with crypto wallets (no real money at stake)
- Centralized with no transparency on fairness or fund custody
- Not designed for the Seeker device or its community

**Existing Blockchain Games:**
- Require 5-10+ transactions for a single game session (create, commit, reveal, distribute)
- Transaction confirmation times break the flow and excitement
- Complex UI/UX designed for "crypto natives" not casual users
- Often require external wallet connections (Phantom, etc.) adding friction

**P2P Betting Solutions:**
- Manual address exchanges are cumbersome and error-prone
- No standardized escrow mechanism (trust issues)
- No discovery mechanism for finding available games or players
- Lack real-time gameplay feedback

**None of these solutions optimize for the specific context: two people physically together, wanting to play a quick game with a small bet, using their Seeker devices.**

### Proposed Solution

**seeker-rps** delivers instant rock-paper-scissors gameplay with real SOL bets through a hybrid architecture that maximizes UX while maintaining blockchain security benefits.

**Core Experience:**
1. **Game Creation** (5 seconds): Player 1 opens app, selects bet amount (e.g., 0.1 SOL), confirms one transaction to create game and deposit funds into escrow, receives a simple 6-character game code
2. **Game Join** (5 seconds): Player 2 enters code (manually, QR scan, or future NFC tap), sees bet details, confirms one transaction to join and deposit matching funds
3. **Instant Gameplay** (3 seconds): Both players select rock/paper/scissors simultaneously, choices are committed offchain via WebSocket to backend, instant reveal shows winner
4. **Automatic Payout** (immediate): Backend instructs smart contract to distribute escrowed funds to winner, no additional user transaction required

**Technical Architecture:**
- **Onchain (Rust + Anchor)**: Game state, escrow accounts, fund distribution logic with backend authority signature verification, emergency withdraw timelock for safety
- **Offchain (Rust backend)**: Game code mapping (short codes to game addresses), commit-reveal gameplay logic, real-time WebSocket connections, audit logs for transparency
- **Mobile Client (Kotlin + Seeker Wallet Adapter)**: Native Seeker integration, intuitive UI, WebSocket for real-time updates, game code exchange mechanisms

**Key Design Principles:**
- **Minimize Transactions**: Only 2 per game (deposits), gameplay is free and instant
- **Backend as Router**: Centralized component only handles non-critical functions (code mapping, gameplay coordination), funds always secured onchain
- **Auditability**: Complete game logs stored for community verification
- **Safety First**: Emergency withdraw function if backend fails, transparent outcome calculation

### Key Differentiators

**1. Seeker-Native Integration**
- Built specifically for the Seeker device with integrated wallet
- No external wallet connection needed (Phantom, etc.)
- Leverages existing Seeker community and distribution

**2. Web2-Level UX with Web3 Benefits**
- Only 2 blockchain transactions per game (vs 5-10+ in traditional blockchain games)
- Instant gameplay with no confirmation waiting
- Simple 6-character codes instead of 44-character addresses
- But maintains trustless escrow and emergency recovery

**3. Optimized for Physical Proximity**
- Designed for face-to-face social interactions (events, meetups, bars)
- Game code exchange designed for co-located players
- Instant feedback loop encourages repeated plays and social engagement

**4. Pragmatic Trust Model**
- Backend handles non-critical functions (gameplay coordination)
- Smart contract handles critical functions (fund custody and distribution)
- Full audit trail for transparency
- Emergency withdrawals for worst-case scenarios
- Appropriate for low-stakes casual gaming (0.1-1 SOL range)

**5. Developer-Friendly Foundation**
- Modern tech stack (Kotlin, Rust backend, Rust/Anchor)
- Clean separation of concerns (client, backend, blockchain)
- Extensible architecture for future features (tournaments, matchmaking, leaderboards)
- Fast iteration cycle for MVP and beyond

**Why Now:**
- Seeker devices are in market with growing community
- Solana fees and speed make micro-betting viable
- Crypto adoption moving toward casual, social use cases
- Gap in market for simple, fun blockchain experiences

---

## References

- **Solana Mobile (Seeker) — Documentation:** [https://docs.solanamobile.com/](https://docs.solanamobile.com/) — SDK, Mobile Wallet Adapter, Seed Vault, and guides for Kotlin, React Native, Flutter, etc.
