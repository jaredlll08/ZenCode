package org.openzen.zenscript.json;

import com.google.gson.*;
import org.openzen.zencode.shared.CodePosition;
import org.openzen.zenscript.codemodel.*;
import org.openzen.zenscript.codemodel.annotations.AnnotationDefinition;
import org.openzen.zenscript.codemodel.definition.ClassDefinition;
import org.openzen.zenscript.codemodel.definition.ZSPackage;
import org.openzen.zenscript.codemodel.generic.TypeParameter;
import org.openzen.zenscript.codemodel.identifiers.ModuleSymbol;
import org.openzen.zenscript.codemodel.member.ConstructorMember;
import org.openzen.zenscript.codemodel.member.GetterMember;
import org.openzen.zenscript.codemodel.member.MethodMember;
import org.openzen.zenscript.codemodel.type.*;
import org.openzen.zenscript.lexer.ParseException;
import org.openzen.zenscript.parser.BracketExpressionParser;
import org.openzen.zenscript.parser.ModuleLoader;
import org.openzen.zenscript.parser.logger.ParserLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonModule {

	public static class JsonSymbol {
		private final String name;

		private final JsonObject json;
		private final HighLevelDefinition symbol;

		public JsonSymbol(String name, JsonObject json, HighLevelDefinition symbol) {
			this.name = name;
			this.json = json;
			this.symbol = symbol;
		}

		public String name() {
			return name;
		}

		public JsonObject json() {
			return json;
		}

		public HighLevelDefinition symbol() {
			return symbol;
		}
	}

	public static class Loader implements ModuleLoader {

		public static final Gson GSON = new GsonBuilder().create();
		private final List<Path> files = new ArrayList<>();

		public Loader() {
		}

		@Override
		public SemanticModule loadModule(ModuleSpace space, String name, BracketExpressionParser bracketParser, SemanticModule[] dependencies, FunctionParameter[] scriptParameters, ZSPackage pkg, ParserLogger logger) throws ParseException {
			try {
				ModuleSymbol json1 = new ModuleSymbol("json");
				PackageDefinitions packageDefinitions = new PackageDefinitions();
				Path path = Paths.get("X:", "LSP", "docsOut", "docs");
				// map of java key to TypeSymbol
				Map<String, JsonSymbol> definitions = new HashMap<>();

				try (Stream<Path> fileStream = Files.walk(path)) {
					List<Path> walk = fileStream.collect(Collectors.toList());
					for (Path path1 : walk) {
						if (Files.isDirectory(path1)) {
							continue;
						}
						try (BufferedReader reader = Files.newBufferedReader(path1)) {
							JsonObject json = GSON.fromJson(reader, JsonObject.class);
							if (json.get("page_type").getAsString().equals("type")) {
								String javaKey = json.get("type").getAsJsonObject().get("key").getAsString();
								String zenCodeName = json.get("zen_code_name").getAsString();
								ZSPackage classPackage = pkg.parent;
								String[] split = zenCodeName.split("\\.");
								for (int i = 0; i < split.length - 1; i++) {
									classPackage = classPackage.getOrCreatePackage(split[i]);
								}

								ClassDefinition definition = new ClassDefinition(CodePosition.GENERATED, json1, classPackage, split[split.length - 1], Modifiers.NONE, null);
								definitions.put(javaKey, new JsonSymbol(javaKey, json, definition));
							}
						}
					}
				}

				for (JsonSymbol symbol : definitions.values()) {
					HighLevelDefinition definition = symbol.symbol();
					JsonObject json = symbol.json();

					ConstructorMember member = new ConstructorMember(CodePosition.GENERATED, definition, Modifiers.PUBLIC.with(Modifiers.EXTERN), FunctionHeader.PLACEHOLDER);
					definition.addMember(member);
					if (json.has("member_groups")) {

						JsonObject memberGroups = json.getAsJsonObject("member_groups");
						memberGroups.keySet().stream().map(memberGroups::getAsJsonObject).map(jsonObject -> jsonObject.getAsJsonArray("members")).forEach(jsonArray -> {
							for (JsonElement jsonElement : jsonArray) {
								JsonObject jsonObject = jsonElement.getAsJsonObject();
								String memberType = jsonObject.get("member_type").getAsString();
								JsonObject returnTypeJson = jsonObject.getAsJsonObject("return_type");
								JsonArray paramsJson = jsonObject.getAsJsonArray("parameters");

								if (memberType.equals("method")) {
									TypeID returnType = resolveType(space, definitions, returnTypeJson);
									FunctionHeader header = FunctionHeader.PLACEHOLDER.withReturnType(returnType);
									if (paramsJson != null && !paramsJson.isEmpty()) {
										header = new FunctionHeader(returnType, StreamSupport.stream(paramsJson.spliterator(), false).map(element -> new FunctionParameter(resolveType(space, definitions, element.getAsJsonObject().getAsJsonObject("type")), element.getAsJsonObject().get("key").getAsString())).toArray(FunctionParameter[]::new));
									}
									MethodMember key = new MethodMember(CodePosition.GENERATED, definition, Modifiers.PUBLIC, jsonObject.getAsJsonObject().get("key").getAsString(), header);
									definition.addMember(key);
								} else if (memberType.equals("getter")) {

									GetterMember key = new GetterMember(CodePosition.GENERATED, definition, Modifiers.PUBLIC, jsonObject.getAsJsonObject().get("key").getAsString(), resolveType(space, definitions, returnTypeJson));
									definition.addMember(key);
								}
							}
						});
					}
					packageDefinitions.add(definition);
				}

				SemanticModule semanticModule = new SemanticModule(new ModuleSymbol("crafttweaker"), SemanticModule.NONE, FunctionParameter.NONE, SemanticModule.State.ASSEMBLED, space.rootPackage, pkg, packageDefinitions, Collections.emptyList(), space.collectExpansions(), space.getAnnotations().toArray(new AnnotationDefinition[0]), logger);
				return semanticModule.normalize();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private TypeID resolveType(ModuleSpace space, Map<String, JsonSymbol> definitions, JsonObject json) {
			String typeType = json.get("type_type").getAsString();
			boolean nullable = json.get("nullable").getAsBoolean();
			String javaKey = json.get("key").getAsString();
			TypeID returnType = BasicTypeID.UNDETERMINED;
			if (typeType.equals("array")) {
				returnType = new ArrayTypeID(resolveType(space, definitions, json.get("type").getAsJsonObject()));
			}
			if (returnType == BasicTypeID.UNDETERMINED && typeType.equals("generic")) {
				returnType = new GenericTypeID(new TypeParameter(CodePosition.GENERATED, json.get("key").getAsString()));
			}

			if (returnType == BasicTypeID.UNDETERMINED && definitions.containsKey(javaKey)) {
				returnType = DefinitionTypeID.create(definitions.get(javaKey).symbol());
			}
			if (returnType == BasicTypeID.UNDETERMINED) {
				if (javaKey.equals("java.util.Map")) {
					JsonObject jsonObject = json.get("type_parameters").getAsJsonObject();
					returnType = new AssocTypeID(resolveType(space, definitions, jsonObject.getAsJsonObject("K")), resolveType(space, definitions, jsonObject.getAsJsonObject("V")));
				}
				if (javaKey.equals("java.lang.String")) {
					returnType = BasicTypeID.STRING;
				}
				if (javaKey.equals("java.util.List")) {
					JsonObject jsonObject = json.get("type_parameters").getAsJsonObject();
					returnType = DefinitionTypeID.create(space.getModule("stdlib").definitions.getDefinition("List"), resolveType(space, definitions, jsonObject.getAsJsonObject("E")));
				}
				if (javaKey.equals("java.lang.Iterable")) {
					JsonObject jsonObject = json.get("type_parameters").getAsJsonObject();
					returnType = DefinitionTypeID.create(space.getModule("stdlib").definitions.getDefinition("Iterable"), resolveType(space, definitions, jsonObject.getAsJsonObject("T")));
				}
				if (javaKey.equals("java.util.function.Predicate")) {
					//TODO
					returnType = new FunctionTypeID(FunctionHeader.PLACEHOLDER);
				}
				if (javaKey.equals("boolean")) {
					returnType = BasicTypeID.BOOL;
				} else {
					for (BasicTypeID value : BasicTypeID.values()) {
						if (value.name.equals(javaKey)) {
							returnType = value;
						}
					}
				}
			}
			if (returnType == BasicTypeID.UNDETERMINED) {
				returnType = BasicTypeID.INVALID;
			}
			return nullable ? new OptionalTypeID(returnType) : returnType;
		}
	}
}
