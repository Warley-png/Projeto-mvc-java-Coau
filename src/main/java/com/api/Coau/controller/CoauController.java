package com.api.coau.controller;

import com.api.Coau.model.AluguelLivro;
import com.api.Coau.model.Cliente;
import com.api.Coau.model.ClienteRepository;
import com.api.Coau.model.Livro;
import com.api.Coau.model.LivroRepository;
import com.api.Coau.model.aluguelLivroRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/livros")
public class CoauController {

    @Autowired
    private LivroRepository repository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private aluguelLivroRepository aluguelLivroRepository;

    @GetMapping("/telaprincipal")
    public String telaPrincipal(Model model, HttpSession session) {
        String usuarioLogado = (String) session.getAttribute("usuarioLogado");
        model.addAttribute("usuarioLogado", usuarioLogado != null ? usuarioLogado : "Usuário");
        return "telaprincipal";
    }

    @GetMapping("/lista")
    public String listar(Model model) {
        List<Livro> livros = repository.findAll();
        model.addAttribute("livros", livros);
        return "lista-livros";  // Mapeia para lista-livros
    }

    @GetMapping("/disponiveis")
    public String listarDisponiveis(Model model) {
        List<Livro> livros = repository.findByDisponivelTrue();
        model.addAttribute("livros", livros);
        model.addAttribute("mensagem", "Apenas livros disponíveis listados.");
        return "lista-livros";
    }

    @GetMapping("/cadastro-livros")
    public String cadastroForm(Model model) {
        System.out.println("Tentando acessar /cadastro-livros");
        model.addAttribute("livro", new Livro());
        return "cadastro-livros";
    }

    @PostMapping("/cadastro")
    public String salvar(@Valid @ModelAttribute Livro livro, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "cadastro-livros";
        }
        repository.save(livro);
        redirectAttributes.addFlashAttribute("mensagem", "Livro cadastrado com sucesso!");
        return "redirect:/livros/listaLivro";  
    }

    @GetMapping("/listaLivro")
    public String listarLivros(Model model) {
        List<Livro> livros = repository.findAll();
        model.addAttribute("livros", livros);

        return "listaLivro";

    }

    @GetMapping("/editarLivro/{id}")
    public String editarLivro(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Livro> livroOpt = repository.findById(id);

        if (livroOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Livro não encontrado!");
            return "redirect:/livros/listaLivro"; 
        }

        model.addAttribute("livro", livroOpt.get());
        return "cadastro-livros"; 
    }

    @PostMapping("/excluirLivro/{id}")
    public String excluirLivro(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            repository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagem", "Livro excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao excluir Livro: " + e.getMessage());
        }
        return "redirect:/livros/listaLivro";
    }

    @GetMapping("/cadastroCliente")
    public String cadastroClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente()); 
        model.addAttribute("clientes", clienteRepository.findAll()); 
        return "cadastroCliente";
    }

    @PostMapping("/cliente")
    public String salvarCliente(@Valid @ModelAttribute Cliente cliente, BindingResult result, RedirectAttributes redirectAttributes) {
        if (clienteRepository.findByEmailCliente(cliente.getEmailCliente()).isPresent()) {
            result.rejectValue("emailCliente", "error.cliente", "Email já cadastrado.");
        }
        if (result.hasErrors()) {
            return "cadastroCliente";
        }
        clienteRepository.save(cliente);
        redirectAttributes.addFlashAttribute("mensagem", "Cliente cadastrado com sucesso!");
        return "redirect:/livros/cadastroCliente"; 
    }

    @GetMapping("/lista-clientes")
    public String listarClientes(Model model) {
        List<Cliente> clientes = clienteRepository.findAll();
        model.addAttribute("clientes", clientes);
        return "lista-clientes";
    }

    @GetMapping("/editarCliente/{id}")
    public String editarCliente(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        if (clienteOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado!");
            return "redirect:/livros/cadastroCliente";  
        }
        Cliente cliente = clienteOpt.get();
        model.addAttribute("cliente", cliente);
        model.addAttribute("clientes", clienteRepository.findAll()); 
        return "cadastroCliente"; 
    }

    @PostMapping("/excluirCliente/{id}")
    public String excluirCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            clienteRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagem", "Cliente excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao excluir cliente: " + e.getMessage());
        }
        return "redirect:/livros/cadastroCliente";
    }

    @GetMapping("/emprestimo")  
    public String telaEmprestimo(Model model) {
        System.out.println("Endpoint GET /livros/emprestimo chamado!");  // Log para debug
        List<Livro> livros = repository.findAll();
        List<Cliente> clientes = clienteRepository.findAll();
        model.addAttribute("livros", livros);
        model.addAttribute("clientes", clientes);

        System.out.println("Livros carregados: " + livros.size());
        return "emprestimo";  
    }

    @PostMapping("/emprestar")  
    public String emprestarLivro(@RequestParam Long livroId, @RequestParam Long clienteId, RedirectAttributes redirectAttributes) {
        try {
            Livro livro = repository.findById(livroId)
                    .orElseThrow(() -> new RuntimeException("Livro não encontrado"));
            Cliente cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
            if (!livro.isDisponivel()) {
                redirectAttributes.addFlashAttribute("erro", "Livro não disponível.");
                return "redirect:/livros/emprestimo";
            }
            AluguelLivro aluguel = new AluguelLivro();
            aluguel.setLivro(livro);
            aluguel.setCliente(cliente);
            aluguel.setDataEmprestimo(LocalDate.now());
            aluguel.setStatus("EMPRESTADO");
            aluguelLivroRepository.save(aluguel);
            livro.setDisponivel(false);
            repository.save(livro);
            redirectAttributes.addFlashAttribute("mensagem", "Livro alugado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao alugar livro: " + e.getMessage());
        }
        return "redirect:/livros/emprestimo";
    }

    @GetMapping("/lista-emprestimo")
    public String listarEmprestimos(Model model) {
        List<AluguelLivro> emprestimos = aluguelLivroRepository.findAll();  
        model.addAttribute("emprestimos", emprestimos);
        return "lista-emprestimo";  
    }

    @PostMapping("/devolver/{id}")
    public String devolverLivro(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<AluguelLivro> emprestimoOpt = aluguelLivroRepository.findById(id);
        if (emprestimoOpt.isPresent()) {
            AluguelLivro emprestimo = emprestimoOpt.get();
            
            emprestimo.getLivro().setDisponivel(true);
            repository.save(emprestimo.getLivro());  
            
            aluguelLivroRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagem", "Livro devolvido com sucesso!");
        } else {
            redirectAttributes.addFlashAttribute("erro", "Empréstimo não encontrado!");
        }
        return "redirect:/livros/lista-emprestimo";
    }

}
