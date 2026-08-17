import { Footer } from "@/components/layout/Footer";
import { Navbar } from "@/components/layout/Navbar";
import { AccessAreas } from "@/components/landing/AccessAreas";
import { Benefits } from "@/components/landing/Benefits";
import { ContactCta } from "@/components/landing/ContactCta";
import { Hero } from "@/components/landing/Hero";
import { HowItWorks } from "@/components/landing/HowItWorks";
import { Platform } from "@/components/landing/Platform";
import { Programs } from "@/components/landing/Programs";
import { WhyChoose } from "@/components/landing/WhyChoose";

export default function Home() {
  return (
    <>
      <Navbar />
      <main className="flex-1">
        <Hero />
        <Benefits />
        <Programs />
        <Platform />
        <AccessAreas />
        <HowItWorks />
        <WhyChoose />
        <ContactCta />
      </main>
      <Footer />
    </>
  );
}
