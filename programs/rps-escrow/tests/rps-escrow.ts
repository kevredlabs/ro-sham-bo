import { assert, expect } from "chai";
import * as anchor from "@coral-xyz/anchor";
import { AnchorError, Program } from "@coral-xyz/anchor";
import { RpsEscrow } from "../target/types/rps_escrow";
import * as fs from "fs";
import * as path from "path";

/**
 * How to use custom Anchor errors (from errors.rs):
 * - Catch the error and check `error instanceof AnchorError`.
 * - anchorError.error.errorCode.code  → enum variant name (e.g. "Unauthorized", "InvalidWinner").
 * - anchorError.error.errorCode.number → error number (ordinal).
 * - anchorError.error.errorMessage     → full message from #[msg(...)].
 */

const INITIAL_BALANCE = 100 * anchor.web3.LAMPORTS_PER_SOL;

async function airdropTo(
  provider: anchor.AnchorProvider,
  lamports: number,
  ...wallets: anchor.web3.PublicKey[]
): Promise<void> {
  const { blockhash, lastValidBlockHeight } =
    await provider.connection.getLatestBlockhash();
  for (const wallet of wallets) {
    const sig = await provider.connection.requestAirdrop(wallet, lamports);
    await provider.connection.confirmTransaction({
      signature: sig,
      blockhash,
      lastValidBlockHeight,
    });
  }
}

function loadKeypair(name: string): anchor.web3.Keypair {
  const keypath = path.join(__dirname, "..", name);
  const keypairData = JSON.parse(fs.readFileSync(keypath, "utf-8"));
  return anchor.web3.Keypair.fromSecretKey(Uint8Array.from(keypairData));
}

describe("create game, deposit, join game, resolve game (happy path)", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  console.log("Authority:", authority.publicKey.toBase58());
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "Game escrow should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("Resolve game!", async () => {
    const tx = await program.methods
    .resolve(winner.publicKey)
    .accountsStrict({
      authority: authority.publicKey,
      gameEscrow: gameEscrowPda,
      vault:vaultPda,
      winnerDestination: creator.publicKey,
      creator: creator.publicKey,
      systemProgram: anchor.web3.SystemProgram.programId,
      treasury: treasury
    })
    .signers([authority])
    .rpc();

    console.log("Your transaction signature", tx);
    const gameEscrowPdaInfo = await provider.connection.getAccountInfo(gameEscrowPda);
    const vaultPdaInfo = await provider.connection.getAccountInfo(vaultPda);
    assert.isNull(gameEscrowPdaInfo, "Game escrow PDA should be null");
    assert.isNull(vaultPdaInfo, "Vault PDA should be null");
    assert.isAbove(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE, "Creator should have win");
    assert.isAbove(await provider.connection.getBalance(treasury),0, "Treasury should have the 3% fee");

  })

});


describe("try to create 2 same games", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Should fail to create the same game again!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();
    console.log("Your transaction signature", tx);

    try {
      const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("already in use"), `Expected "already in use" in: ${message}`);
    }


  });
});


describe("create game, deposit, join game and try to resolve game with the wrong authority", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("should fail to resolve game with the wrong authority!", async () => {

    try {
      await program.methods
        .resolve(winner.publicKey)
        .accountsStrict({
          authority: creator.publicKey,
          gameEscrow: gameEscrowPda,
          vault:vaultPda,
          creator: creator.publicKey,
          systemProgram: anchor.web3.SystemProgram.programId,
          winnerDestination: creator.publicKey,
          treasury: treasury
        })
        .signers([creator])
        .rpc();
      expect.fail("Expected error");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("Unauthorized"), `Expected "Unauthorized" in: ${message}`);
    }

  });


});

describe("create game with amount = 0 ", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Should fail to create game with amount = 0!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    try {
      await program.methods
      .createGame(Array.from(gameId), new anchor.BN(0))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();
      expect.fail("Expected error");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("InvalidAmount"), `Expected "InvalidAmount" in: ${message}`);
    }
  });

});




describe("create game, deposit and try to resolve without joiner ", () => {
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Should fail to resolve game without joiner!", async () => {

    try {
      await program.methods
        .resolve(winner.publicKey)
        .accountsStrict({
          authority: authority.publicKey,
          gameEscrow: gameEscrowPda,
          vault:vaultPda,
          creator: creator.publicKey,
          systemProgram: anchor.web3.SystemProgram.programId,
          winnerDestination: creator.publicKey,
          treasury: treasury
        })
        .signers([authority])
        .rpc();
      expect.fail("Expected error");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("NoJoiner"), `Expected "NoJoiner" in: ${message}`);
    }

  });

});



describe("create game, deposit, join game and try to resolve game with the wrong winner", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("should fail to resolve game with the correct authority but the wrong winner!", async () => {

    try {
      await program.methods
        .resolve(winner.publicKey)
        .accountsStrict({
          authority: authority.publicKey,
          gameEscrow: gameEscrowPda,
          vault:vaultPda,
          creator: creator.publicKey,
          systemProgram: anchor.web3.SystemProgram.programId,
          winnerDestination: authority.publicKey,
          treasury: treasury
        })
        .signers([authority])
        .rpc();
      expect.fail("Expected error");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("InvalidWinner"), `Expected "InvalidWinner" in: ${message}`);
    }
    });


});



describe("create game, deposit, and try to join 2 times the same game", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const joiner2 = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("should fail to join the same game 2 times!", async () => {
    try {
      await program.methods
        .joinGame()
        .accountsStrict({
          joiner: joiner2.publicKey,
          gameEscrow: gameEscrowPda,
          vault:vaultPda,
          systemProgram: anchor.web3.SystemProgram.programId,
        })
        .signers([joiner2])
        .rpc();
      expect.fail("Expected error");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("JoinerAlreadySet"), `Expected "JoinerAlreadySet" in: ${message}`);
    }
  });
});




describe("create game, deposit, join and try to resolve with the correct winner but with a wrong winner destination", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const joiner2 = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;
  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("should fail to resolve with the correct winner but with a wrong winner destination!", async () => {
    try {
      await program.methods
        .resolve(winner.publicKey)
        .accountsStrict({
          authority: authority.publicKey,
          gameEscrow: gameEscrowPda,
          vault:vaultPda,
          creator: creator.publicKey,
          systemProgram: anchor.web3.SystemProgram.programId,
          winnerDestination: authority.publicKey,
          treasury: treasury
        })
        .signers([authority])
        .rpc();
      expect.fail("Expected error");
    } catch (error) { 
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("InvalidWinner"), `Expected "InvalidWinner" in: ${message}`);
    }
  });
});


describe("create game and cancel it before the joiner joins", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const joiner2 = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );


    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });

  it("cancel game!", async () => {
    const tx = await program.methods
    .cancel()
    .accountsStrict({
      creator: creator.publicKey,
      gameEscrow: gameEscrowPda,
      vault:vaultPda,
      systemProgram: anchor.web3.SystemProgram.programId,
    })
    .signers([creator])
    .rpc();
    console.log("Your transaction signature", tx);

    assert.isAbove(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount back");
    assert.isNull(await provider.connection.getAccountInfo(gameEscrowPda), "Game escrow should be closed");
    assert.isNull(await provider.connection.getAccountInfo(vaultPda), "Vault should be closed");

  });

});


describe("create game and try to cancel it after the joiner joins", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const joiner2 = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("should fail to cancel the game after the joiner joins!", async () => {
    try {
      await program.methods
    .cancel()
    .accountsStrict({
      creator: creator.publicKey,
      gameEscrow: gameEscrowPda,
      vault:vaultPda,
      systemProgram: anchor.web3.SystemProgram.programId,
    })
    .signers([creator])
    .rpc();
    expect.fail("Expected error");
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("JoinerAlreadySet"), `Expected "JoinerAlreadySet" in: ${message}`);
    }

  });

});



describe("create game and try to cancel it with the wrong creator", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const joiner2 = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;
  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });

  it("should fail to cancel the game with the wrong creator!", async () => {
    try {
    await program.methods
    .cancel()
    .accountsStrict({
      creator: creator.publicKey,
      gameEscrow: gameEscrowPda,
      vault:vaultPda,
      systemProgram: anchor.web3.SystemProgram.programId,
    })
    .signers([joiner])
    .rpc();
    expect.fail("Expected error");  
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      assert.ok(message.includes("unknown signer"), `Expected "unknown signer" in: ${message}`);
    }
  });
});



describe("create game, deposit, join game and refund both joiner and creator", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("Refund both joiner and creator!", async () => {

    const tx = await program.methods
    .refund()
    .accountsStrict({
      authority: authority.publicKey,
      gameEscrow: gameEscrowPda,
      vault:vaultPda,
      creator: creator.publicKey,
      joiner: joiner.publicKey,
      systemProgram: anchor.web3.SystemProgram.programId,
    })
    .signers([authority])
    .rpc();

    console.log("Your transaction signature", tx);
    const gameEscrowPdaInfo = await provider.connection.getAccountInfo(gameEscrowPda);
    assert.isNull(gameEscrowPdaInfo, "Game escrow PDA should be null");
  })

});





describe("create game, deposit, join game and resolve with wrong treasury", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const wrongTreasury = anchor.web3.Keypair.generate();
  const authority = loadKeypair("resolve_authority.json");
  const treasury = new anchor.web3.PublicKey("Ft6kMwkButM1J7iHJBJTb8QFEBuoBPnG1jq83HMRE9mF");
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  let vaultPda: anchor.web3.PublicKey;
  let vaultBump: number;

  before(async () => {
    await airdropTo(provider, INITIAL_BALANCE, creator.publicKey, joiner.publicKey, authority.publicKey);
  });

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    [vaultPda, vaultBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("vault"), gameEscrowPda.toBuffer()],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();

    assert.isBelow(await provider.connection.getBalance(creator.publicKey),INITIAL_BALANCE - amount, "Creator should have the amount deposited");
    assert.equal(await provider.connection.getBalance(vaultPda), amount, "vault should have the amount deposited");
    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
        vault:vaultPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([joiner])
      .rpc();

    console.log("Your transaction signature", tx);
  });

  it("Should fail to resolve with wrong treasury!", async () => {

    const tx = await program.methods
    .resolve(winner.publicKey)
    .accountsStrict({
      authority: authority.publicKey,
      gameEscrow: gameEscrowPda,
      vault:vaultPda,
      creator: creator.publicKey,
      winnerDestination: creator.publicKey,
      treasury: wrongTreasury.publicKey,
      systemProgram: anchor.web3.SystemProgram.programId,
    })
    .signers([authority])
    .rpc();

    console.log("Your transaction signature", tx);
    const gameEscrowPdaInfo = await provider.connection.getAccountInfo(gameEscrowPda);
    assert.isNull(gameEscrowPdaInfo, "Game escrow PDA should be null");
  })

});
