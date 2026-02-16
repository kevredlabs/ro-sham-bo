import * as anchor from "@coral-xyz/anchor";
import { Program } from "@coral-xyz/anchor";
import { RpsEscrow } from "../target/types/rps_escrow";

describe("rps-escrow", () => {
  // Configure the client to use the local cluster.
  const provider = anchor.AnchorProvider.env();
  anchor.setProvider(provider);

  const program = anchor.workspace.rpsEscrow as Program<RpsEscrow>;
  const creator = anchor.web3.Keypair.generate();
  const joiner = anchor.web3.Keypair.generate();
  const authority = provider.wallet.payer;
  const winner = creator;
  // game_id is [u8; 16] on-chain: UUID without hyphens = 32 hex chars = 16 bytes
  const gameIdStr = "e504f1b02e4e46b08d4189b3b5b47745";
  const gameId = Buffer.from(gameIdStr, "hex");
  const amount = 1_000_000_000; // 1 SOL in lamports

  let gameEscrowPda: anchor.web3.PublicKey;
  let gameEscrowBump: number;

  console.log("creator", creator.publicKey.toBase58());
  console.log("joiner", joiner.publicKey.toBase58());
  console.log("authority", authority.publicKey.toBase58());
  console.log("gameId", gameIdStr);
  console.log("amount", amount);

  before(async () => {
    console.log("Airdropping to creator and joiner");
    const latestBlockHash = await provider.connection.getLatestBlockhash();
    const signature1 = await provider.connection.requestAirdrop(creator.publicKey, 100*anchor.web3.LAMPORTS_PER_SOL);
    const signature2 = await provider.connection.requestAirdrop(joiner.publicKey, 100*anchor.web3.LAMPORTS_PER_SOL);

    await provider.connection.confirmTransaction({
      signature: signature1,
      blockhash: latestBlockHash.blockhash,
      lastValidBlockHeight: latestBlockHash.lastValidBlockHeight,
    });
    await provider.connection.confirmTransaction({
      signature: signature2,
      blockhash: latestBlockHash.blockhash,
      lastValidBlockHeight: latestBlockHash.lastValidBlockHeight,
  });
});

  it("Create game and deposit!", async () => {
    [gameEscrowPda, gameEscrowBump] = anchor.web3.PublicKey.findProgramAddressSync(
      [Buffer.from("game_escrow"), creator.publicKey.toBuffer(), gameId],
      program.programId
    );

    const tx = await program.methods
      .createGame(Array.from(gameId), new anchor.BN(amount))
      .accountsStrict({
        creator: creator.publicKey,
        gameEscrow: gameEscrowPda,
        systemProgram: anchor.web3.SystemProgram.programId,
      })
      .signers([creator])
      .rpc();



    console.log("Your transaction signature", tx);
  });


  it("Join game and deposit!", async () => {
    const tx = await program.methods
      .joinGame()
      .accountsStrict({
        joiner: joiner.publicKey,
        gameEscrow: gameEscrowPda,
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
      winnerDestination: creator.publicKey,
    })
    .signers([authority])
    .rpc();

    console.log("Your transaction signature", tx);
  })

});