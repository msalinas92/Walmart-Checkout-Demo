import type { Metadata } from "next";
import "./globals.css";
import "./catalog.css";
import "./flow.css";

export const metadata: Metadata = {
  title: "Walmart Demo | Compra y Checkout",
  description: "Demo de catálogo, carrito, promociones y checkout sobre servicios administrados de Google Cloud."
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="es"><body>{children}</body></html>;
}
